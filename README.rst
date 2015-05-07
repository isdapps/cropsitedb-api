==============
CropsiteDB API
==============
:Version: 2.0.0
:Date: 2015-04-22
:Author: Christopher Villalobos

.. contents::


--------
Overview
--------

The CropsiteDB API is used by the AgMIP community to collect and distribute site-based datasets in
AgMIP's model-ready format. It also allows external data providers to make their metadata available
to the AgMIP community (after collaborating on a common dictionary) through this API.

This version of the CropsiteDB API supports the following features:

Datasets
    The ability to create datasets consisting of ACEB, DOME, ALNK, and ACMO files.

Querying
    Query on ACEB (input) metadata to retrieve datasets.

Uploading

Downloading

There is a lot to be done yet still, a roadmap is soon to follow.

------------
Requirements
------------

SQL Database for Metadata Storage
    MySQL/MariaDB supported
    PostgreSQL supported

Java JRE 7 or 8

Local Filesystem for Data Storage

----------------------------
Using CropSiteDB from Source
----------------------------

*Coming Soon*

--------------------
Deploying CropsiteDB
--------------------

1. Download and unzip the `cropsitedb-api-2.0.1.zip <http://tools.agmip.org/download/cropsitedb-api-2.0.1.zip>`_

2. Copy the conf/application.conf.sample to application.conf in a directory above the application root
    This is to make it easier to persist application settings over time

3. Modify the application.conf to suit your environment
    Helpful URLS
        https://www.playframework.com/documentation/2.3.x/Configuration

        https://www.playframework.com/documentation/2.3.x/ScalaDatabase

4.  Run the server

.. code-block:: bash

	$ <application_root>/bin/cropsitedb-api -Dconfig.file=<path to application.conf> [-Dhttp.port=<port>] &


5. Everything should be running! 
