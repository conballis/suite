.. _intro.installation.windows.minorupdate:

Updating a minor version
========================
   
This section describes how to perform a minor update from OpenGeo Suite 4.x to |version| on Windows.

.. note::

   * For new installations, please see the section on :ref:`intro.installation.windows.install`.
   * For upgrading to **OpenGeo Suite Enterprise**, please see the section on :ref:`intro.installation.windows.upgrade`.
   * For updating from a previous **major version** of OpenGeo Suite (3.x), please see the :ref:`intro.installation.windows.majorupdate` section.

.. include:: include/sysreq.txt

Installation
------------

.. warning:: OpenGeo Suite for Windows requires `.NET Framework 4 <http://www.microsoft.com/en-us/download/details.aspx?id=17851>`_. Installation will fail if not present.

#. Shutdown all OpenGeo Suite services by navigating to :menuselection:`Start Menu --> All Programs --> OpenGeo Suite` and using the :guilabel:`Stop` shortcuts. Alternately, services can be stopped from the Windows :guilabel:`Services` dialog, and stopping both the **OpenGeo Jetty** and **OpenGeo PostgreSQL** services. 

   .. figure:: img/startstop_services.png

      Starting and stopping OpenGeo Suite services

   Services can also be controlled from the Windows :guilabel:`Services` dialog available by navigating to :menuselection:`Administrative Tools --> Services` from the Windows :guilabel:`Control Panel`.

#. Double-click the :file:`OpenGeoSuite.exe` file.

#. At the **Welcome** screen, click :guilabel:`Next`.

   .. only:: basic

      .. figure:: img/welcome.png

         Welcome screen

   .. only:: enterprise

      .. figure:: img/welcome-ent.png

         Welcome screen

#. The installer will recognize that an existing version of OpenGeo Suite on the system, and will warn you about the update.

   .. note:: For upgrading to **OpenGeo Suite Enterprise**, please see the section on :ref:`intro.installation.windows.upgrade`.

   .. only:: basic

      .. figure:: img/update.png

         Setup recognizing an existing OpenGeo Suite instance

   .. only:: enterprise

      .. figure:: img/update-ent.png

         Setup recognizing an existing OpenGeo Suite instance

#. Read the **License Agreement** then click :guilabel:`I Agree`.

   .. only:: basic

      .. figure:: img/license.png

         License agreement

   .. only:: enterprise

      .. figure:: img/license-ent.png

         License agreement

#. Select the **Destination folder** where you would like to install OpenGeo Suite, and click :guilabel:`Next`.

   .. only:: basic

      .. figure:: img/directory.png

         Destination folder for the installation

   .. only:: enterprise

      .. figure:: img/directory-ent.png

         Destination folder for the installation

#. Select the name and location of the **Start Menu folder** to be created, and click :guilabel:`Next`.

   .. only:: basic

      .. figure:: img/startmenu.png

         Start Menu folder to be created

   .. only:: enterprise

      .. figure:: img/startmenu-ent.png

         Start Menu folder to be created

#. Select the components you wish to install, and click :guilabel:`Next`.

   .. only:: basic

      .. figure:: img/components.png

         Component selection

   .. only:: enterprise

      .. figure:: img/components-ent.png

         Component selection

   .. note:: See :ref:`intro.installation.windows.components` for more details on available components.

#. Click :guilabel:`Install` to perform the installation.

   .. only:: basic

      .. figure:: img/ready.png

         Ready to install

   .. only:: enterprise

      .. figure:: img/ready-ent.png

         Ready to install

#. Please wait while the installation proceeds.

   .. only:: basic

      .. figure:: img/install.png

         Installation

   .. only:: enterprise

      .. figure:: img/install-ent.png

         Installation

#. After installation, click :guilabel:`Finish`.

   .. only:: basic

      .. figure:: img/finish.png

         OpenGeo Suite successfully installed

   .. only:: enterprise

      .. figure:: img/finish.png

         OpenGeo Suite successfully installed

After update
------------

The update is now complete. Please see the section on :ref:`intro.installation.windows.postinstall` to continue.
