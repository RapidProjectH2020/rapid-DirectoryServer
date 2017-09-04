# The Directory Server for the RAPID project

In the RAPID architecture, the Directory Server (DS) centralizes the knowledge of computational resources in the cloud infrastructure. The DS targets to collect the information of clients, Virtual Machine Managers (VMMs), virtual machines (VMs), and Acceleration Servers (ASs) in the RAPID project.

To install the DS, the MySQL database needs to be installed in advance, since the DS uses the database for persistency.

Before building the source code, the user may need to change the configuration files according to the local system. In the “rapid_ds/src” folder, there are two configuration files to modify: db.properties and configuration.xml. The first file, db.properties, contains the DB connection information, where the user and password information have to be modified accordingly. The DS part of configuration.xml contains socket server configuration settings. The transcription of the file is as follows:

* vmmPort denotes the server port that the VMM uses.
* dsMaxConnection indicates the size of the thread pool that the ThreadPooledServer class creates.
* dsPort denotes the server port that the DS uses.
* dsIpAddress denotes the IP address of the DS.

Now, we need to setup the database. To create database tables and initialize them automatically, this command is required in the “rapid_ds” directory:

mysql -uUser -pPassword < ./ rapid_ds.sql;

where User and Password should be replaced with the information of the database account.

To simplify the installation process, we use Ant to compile the source code. Ant reads the build.xml file in the “rapid_ds” directory and sets up the compiling environment. To compile the source code, just typing “$ant” in the “rapid_ds” directory is required. The source code of the DS will be then compiled and the binary files will be placed at the “rapid_ds/bin” directory.

The “java eu.rapid.ds.DirectoryServer” command in the bin directory will execute the main function of the DS after compilation. To execute the DS, the jar files at the lib directory need to be added to the Java classpath.
