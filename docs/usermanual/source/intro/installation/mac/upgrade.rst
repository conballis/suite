.. _intro.installation.mac.upgrade:

Upgrading to OpenGeo Suite Enterprise
=====================================

This section describes how to upgrade OpenGeo Suite to **OpenGeo Suite Enterprise** on Mac OS X.

.. note:: OpenGeo Suite Enterprise can only be obtained through `Boundless <http://boundlessgeo.com>`_. Please `contact us <http://boundlessgeo.com/about/contact-us/sales/>`_ for information on how to purchase OpenGeo Suite Enterprise.

.. include:: include/sysreq.txt

Upgrade process
---------------

#. To upgrade, you must first back up your data:
   
   * Back up your GeoServer data directory by copying it to a safe location. You can find your data directory by running GeoServer and selecting :guilabel:`Open GeoServer Data Directory` from the GeoServer icon in the OS X menu bar.
   * Back up your PostGIS database to a safe location by following the instructions in the :ref:`dataadmin.pgDBAdmin.backup` section.

#. After backing up your data, :ref:`uninstall <intro.installation.mac.uninstall>` your current version of OpenGeo Suite.

#. The Mac installer for OpenGeo Suite is a disk image made up several :ref:`components <intro.installation.mac.components>` that are installed separately. 

   .. only:: basic

      .. figure:: img/apps-basic.png

         OpenGeo Suite for Mac OS X

   .. only:: enterprise

      .. figure:: img/apps-ee.png

         OpenGeo Suite for Mac OS X

.. include:: include/componentinstall.txt

After upgrade
-------------

The upgrade is now complete. Please see the section on :ref:`intro.installation.mac.postinstall` to continue.
