/* (c) 2014-2015 Boundless, http://boundlessgeo.com
 * This code is licensed under the GPL 2.0 license.
 */
package com.boundlessgeo.geoserver.api.controllers;

import com.boundlessgeo.geoserver.api.exceptions.BadRequestException;
import com.boundlessgeo.geoserver.api.exceptions.NotFoundException;
import com.boundlessgeo.geoserver.json.JSONArr;
import com.boundlessgeo.geoserver.json.JSONObj;
import com.boundlessgeo.geoserver.util.Hasher;
import com.google.common.collect.Maps;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.importer.Database;
import org.geoserver.importer.Directory;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportFilter;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.Table;
import org.geoserver.platform.resource.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/imports")
public class ImportController extends ApiController {

    Importer importer;
    Hasher hasher;

    @Autowired
    public ImportController(GeoServer geoServer, Importer importer) {
        super(geoServer);
        this.importer = importer;
        this.hasher = new Hasher(7);
    }

    @RequestMapping(value = "/{wsName}", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public @ResponseBody
    JSONObj importFile(@PathVariable String wsName, HttpServletRequest request)
        throws Exception {

        // grab the workspace
        Catalog catalog = geoServer.getCatalog();
        WorkspaceInfo ws = findWorkspace(wsName, catalog);

        // get the uploaded files
        Iterator<FileItem> files = doFileUpload(request);
        if (!files.hasNext()) {
            throw new BadRequestException("Request must contain a single file");
        }

        // create a new temp directory for the uploaded file
        File uploadDir = dataDir().get(ws, "data", hasher.get().toLowerCase()).dir();
        if (!uploadDir.exists()) {
            throw new RuntimeException("Unable to create directory for file upload");
        }

        // pass off the uploaded file to the importer
        Directory dir = new Directory(uploadDir);
        dir.accept(files.next());

        return doImport(dir, ws);
    }

    @RequestMapping(value = "/{wsName}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody JSONObj importDb(@PathVariable String wsName, @RequestBody JSONObj obj) throws Exception {
        // grab the workspace
        Catalog catalog = geoServer.getCatalog();
        WorkspaceInfo ws = findWorkspace(wsName, catalog);

        // create the import data
        Database db = new Database(hack(obj));
        
        //Initialize the import, and return to requester to allow selection of tables.
        //Complete the import using update()
        return prepImport(db, ws);
    }

    Map<String, Serializable> hack(JSONObj obj) {
        Map<String,Serializable> map = Maps.newLinkedHashMap();
        for (Object e : obj.raw().entrySet()) {
            Map.Entry<String,Serializable> entry = (Map.Entry) e;
            Serializable value = entry.getValue();
            if (value instanceof Long) {
                value = ((Long)value).intValue();
            }
            map.put(entry.getKey(), value);
        }
        return map;
    }
    
    ImportFilter filter(JSONObj obj) {
        Object filter = obj.get("filter");
        if ("ALL".equals(filter)) {
            return ImportFilter.ALL;
        } else {
            JSONArr arr = obj.array("tasks");
            final List<Long> tasks = new ArrayList<Long>(arr.size());
            for (int i = 0; i < arr.size(); i++) {
                tasks.add(Long.parseLong(arr.str(i)));
            }
            return new ImportFilter() {
                @Override
                public boolean include(ImportTask task) {
                    return tasks.contains(task.getId());
                }
            };
        }
    }
    
    /*
     * Set up an import context. 
     * Use continueImport() to complete this import
     * 
     * @param data - The data to import
     * @param ws - The workspace to import into
     * @return JSON representation of the import
     */
    JSONObj prepImport(ImportData data, WorkspaceInfo ws) throws Exception {
        // prepare the import and extract layers
        ImportContext imp = importer.createContext(data, ws);
        return get(ws.getName(), imp.getId());
    }
    
    /*
     * Complete an import started with prepImport().
     * 
     * @param data - The data to import
     * @param ws - The workspace to import into
     * @return JSON representation of the import
     */
    JSONObj continueImport(ImportContext imp, WorkspaceInfo ws, ImportFilter f) throws Exception {
        // run the import
        prepTasks(imp, ws, f);
        importer.run(imp, f);

        for (ImportTask t : imp.getTasks()) {
            if (t.getState() == ImportTask.State.COMPLETE) {
                touch(t);
            }
        }
        imp.setState(ImportContext.State.COMPLETE);
        return get(ws.getName(), imp.getId());
    }

    /*
     * run an import in one step
     * @param data - The data to import
     * @param ws - The workspace to import into
     * @return JSON representation of the import
     */
    JSONObj doImport(ImportData data, WorkspaceInfo ws) throws Exception {
        // run the import
        ImportContext imp = importer.createContext(data, ws);
        imp.setState(ImportContext.State.RUNNING);

        prepTasks(imp, ws);

        importer.run(imp);

        for (ImportTask t : imp.getTasks()) {
            if (t.getState() == ImportTask.State.COMPLETE) {
                touch(t);
            }
        }
        imp.setState(ImportContext.State.COMPLETE);
        return get(ws.getName(), imp.getId());
    }

    void prepTasks(ImportContext imp, WorkspaceInfo ws) {
        prepTasks(imp, ws, ImportFilter.ALL);
    }
    void prepTasks(ImportContext imp, WorkspaceInfo ws, ImportFilter f) {
        // 1. hack the context object to ensure that all styles are workspace local
        // 2. mark layers as "imported" so we can safely delete styles later
        GeoServerDataDirectory dataDir = dataDir();

        for (ImportTask t : imp.getTasks()) {
            //skip any layers we are not importing
            if (f.include(t)) {
                LayerInfo l = t.getLayer();
                l.getMetadata().put(Metadata.IMPORTED, new Date());
    
                if (l != null && l.getDefaultStyle() != null) {
                    StyleInfo s = l.getDefaultStyle();
    
                    // JD: have to regenerate the unique name here, the importer already does this but because we are
                    // putting it into the workspace we have to redo it, this should really be part of the importer
                    // with an option to create styles in the workspace
                    s.setName(findUniqueStyleName(l.getResource(), ws, catalog()));
    
                    Resource from = dataDir.style(s);
    
                    s.setWorkspace(ws);
                    Resource to = dataDir.style(s);
    
                    try {
                        try (
                            InputStream in = from.in();
                            OutputStream out = to.out();
                        ) {
                            IOUtils.copy(in, out);
                            from.delete();
                        }
                    }
                    catch(IOException e){
                        throw new RuntimeException("Error copying style to workspace", e);
                    }
                }
            } else {
                t.setState(ImportTask.State.CANCELED);
            }
        }
    }

    String findUniqueStyleName(ResourceInfo resource, WorkspaceInfo workspace, Catalog catalog) {
        String styleName = resource.getName();
        StyleInfo style = catalog.getStyleByName(workspace, styleName);
        int i = 1;
        while(style != null) {
            styleName = resource.getName() + i;
            style = catalog.getStyleByName(workspace, styleName);
            i++;
        }
        return styleName;
    }

    @RequestMapping(value = "/{wsName}/{id}", method = RequestMethod.GET)
    public @ResponseBody JSONObj get(@PathVariable String wsName, @PathVariable Long id) throws Exception {
        ImportContext imp = findImport(id);

        JSONObj result = new JSONObj();
        result.put("id", imp.getId());

        JSONArr preimport = result.putArray("preimport");
        JSONArr imported = result.putArray("imported");
        JSONArr pending = result.putArray("pending");
        JSONArr failed = result.putArray("failed");
        JSONArr ignored = result.putArray("ignored");

        for (ImportTask task : imp.getTasks()) {
            if (imp.getState() == ImportContext.State.PENDING) {
                preimport.add(task(task));
            } else {
                switch(task.getState()) {
                    case COMPLETE:
                        imported.add(complete(task));
                        break;
                    case NO_BOUNDS:
                    case NO_CRS:
                        pending.add(pending(task));
                        // fixable state, throw into pending
                        break;
                    case ERROR:
                        // error, dump out some details
                        failed.add(failed(task));
                        break;
                    default:
                        // ignore this task
                        ignored.add(ignored(task));
                }
            }
        }

        return result;
    }

    @RequestMapping(value = "/{wsName}/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody JSONObj update(@PathVariable String wsName, @PathVariable Long id, @RequestBody JSONObj obj)
        throws Exception {

        Catalog catalog = geoServer.getCatalog();
        WorkspaceInfo ws = findWorkspace(wsName, catalog);
        
        ImportContext imp = findImport(id);
        //DB Import, select the tables to import
        if (imp.getState() == ImportContext.State.PENDING) {
            ImportFilter f = filter(obj);

            // create the import data
            return continueImport(imp, ws, f);
        }

        Integer t = obj.integer("task");
        if (t == null) {
            throw new BadRequestException("Request must contain task identifier");
        }

        final ImportTask task = imp.task(t);
        if (task == null) {
            throw new NotFoundException("No such task: " + t + " for import: " + id);
        }

        ResourceInfo resource = task.getLayer().getResource();

        if (task.getState() == ImportTask.State.NO_CRS) {
            JSONObj proj = obj.object("proj");
            if (proj == null) {
                throw new BadRequestException("Request must contain a 'proj' property");
            }

            try {
                resource.setSRS(IO.srs(proj));
                resource.setNativeCRS(IO.crs(proj));
                importer.changed(task);
            }
            catch(Exception e) {
                throw new BadRequestException("Unable to parse proj: " + proj.toString());
            }
        }

        importer.run(imp, new ImportFilter() {
            @Override
            public boolean include(ImportTask t) {
                return task.getId() == t.getId();
            }
        });

        if (task.getState() == ImportTask.State.COMPLETE) {
            return complete(task);
        }
        else {
            switch(task.getState()) {
                case NO_CRS:
                case NO_BOUNDS:
                    return pending(task);
                case ERROR:
                    return failed(task);
                default:
                    return ignored(task);
            }
        }
    }

    ImportContext findImport(Long id) {
        ImportContext imp = importer.getContext(id);
        if (imp == null) {
            throw new NotFoundException("No such import: " + id);
        }
        return imp;
    }

    void touch(ImportTask task) {
        LayerInfo l = task.getLayer();
        l = catalog().getLayer(l.getId());
        if (l != null) {
            Date now = new Date();
            Metadata.created(l, now);
            Metadata.modified(l, now);
            geoServer.getCatalog().save(l);
        }
    }

    JSONObj task(ImportTask task) {
        JSONObj obj = new JSONObj();
        obj.put("task", task.getId())
           .put("name", name(task))
           .put("type", type(task));
        return obj;
    }

    JSONObj complete(ImportTask task) {
        touch(task);

        LayerInfo layer = task.getLayer();
        JSONObj obj = task(task);
        IO.layer(obj.putObject("layer"), layer, null);
        return obj;
    }

    JSONObj pending(ImportTask task) {
        return task(task).put("problem", task.getState().toString());
    }

    JSONObj failed(ImportTask task) {
        JSONObj err = task(task);
        IO.error(err, task.getError() );
        return err;
    }

    JSONObj ignored(ImportTask task) {
        return new JSONObj().put("task", task.getId()).put("file", filename(task));
    }
    
    String filename(ImportTask task) {
        ImportData data = task.getData();
        return FilenameUtils.getName(data.toString());
    }
    
    String name(ImportTask task) {
        ImportData data = task.getData();
        if (data instanceof FileData) {
            return FilenameUtils.getName(data.toString());
        }
        return data.getName();
    }
    
    String type(ImportTask task) {
        ImportData data = task.getData();
        if (data instanceof Database) {
            return "database";
        }
        if (data instanceof FileData) {
            return "file";
        }
        if (data instanceof Table) {
            return "table";
        }
        return "null";
    }

}
