# Aneurysm
Aneurysm is a collection of Java plugins for ImageJ http://imagej.nih.gov/ij/ extracting arteries from
MRI images, finding centerlines and measuring tortuosity or twsitedness. The software was developed to 
measure the connection between tortuosity and the development of intracranial aneurysms at the University of Utah. 

## References
### If using this software or deriving works please reference one of the following: 
* Diedrich KT, Roberts JA, Schmidt RH, Parker DL. Comparing Performance of Centerline Algorithms for Quantitative Assessment of Brain Vascular Anatomy. Anat Rec (Hoboken). 2012; doi:10.1002/ar.22603
  * http://www.ncbi.nlm.nih.gov/pubmed/23060363
*  Diedrich KT, Roberts JA, Schmidt RH, Albright LAC, Yetman AT, Parker DL. Medical record and imaging evaluation to identify arterial tortuosity phenotype in populations at risk for intracranial aneurysms. AMIA Annu Symp Proc. 2011;2011: 295–304. 
  * http://www.ncbi.nlm.nih.gov/pubmed/22195081
* Diedrich KT, Roberts JA, Schmidt RH, Kang C-K, Cho Z-H, Parker DL. Validation of an arterial tortuosity measure with application to hypertension collection of clinical hypertensive patients. BMC Bioinformatics. 2011;12 Suppl 10: S15. doi:10.1186/1471-2105-12-S10-S15
  * http://www.ncbi.nlm.nih.gov/pubmed/22166145 

### Additional references using this Aneurysm software
* Wei F, Diedrich KT, Fullerton HJ, deVeber G, Wintermark M, Hodge J, et al. Arterial Tortuosity: An Imaging Biomarker of Childhood Stroke Pathogenesis? Stroke. 2016; doi:10.1161/STROKEAHA.115.011331
  * http://www.ncbi.nlm.nih.gov/pubmed/27006453

## ImageJ plugin
Place the Aneurysm source in ImageJ/plugins/Aneurym

ImageJ needs access to the java interpreter. Use the interpreter withe the JDK if developing. 
http://www.oracle.com/technetwork/java/javase/downloads/index.html
On windows 
\ImageJ\ImageJ.cfg 
Has something like
.
C:\Program Files\Java\jdk\bin\javaw.exe
-Xmx6000m -cp C:\PROGRA~1\Java\jdk\lib\tools.jar;ij.jar ij.ImageJ

On Linux image is started with imagej -c

## Jar files 
ImageJ needs to access Jars. They can be placed in the Java lib/ext directory 
Apache commons CLI for reading command line options
https://commons.apache.org/proper/commons-cli/
MySQL/MariaDB connector 
https://downloads.mariadb.org/connector-java/

Path to Java \Java\jdk\jre\lib\ext
commons-cli-1.2.jar
mysql-connector-java-5.1.18-bin.jar
ij.jar

ij.jar is from the ImageJ installation 

## Database 
An optional database stores stores measurements in MySQL or MariaDB 5.5 https://mariadb.org/ 
-- initializing users
-- mysql -u root -p
-- create database aneurysm;
--  create user 'userone'@'%' identified by 'password';
-- grant all on *.* to 'userone'@'%';

-- connecting to database
-- mysql -u userone -D aneurysm -p

-- reloading backup that will generate all tables 
-- mysql -u userone -D aneurysm -p < sql/aneurysmdump.sql 
-- sql/delete_data.sql will clean the database for a fresh start 

## Compile 
Compile the source code with Ant http://ant.apache.org/bindownload.cgi
The default ant task will build the sources into the directory in build.properties ../Aneurysm-build/Aneurysm 
$ ant 
Adjust build.properties to build into a different directory 

For ImageJ and Java to find the data files fill out and put config/aneurysm.properties in the $Home directory

## R for analysis 
R is used for visualing the data with functions in Aneurysm/R/aneurysm.R
R needs the extra packages
install.packages(MASS) ; install.packages( RMySQL); install.packages(RODBC); install.packages(rgl); 

 for connecting R to mysql 
 rename and add configuration to config/my.cnf and place in  ~/.my.cnf on Linux/UNIX/Mac OS X 
 place in C:\my.cnf for Windows

### Notes on using ODBC and R because RMySQL is not running on 64 bit windows yet 
Packages  
Connector/ODBC 5.1.4 http://dev.mysql.com/downloads/connector/odbc/5.1.html
Control Panel -> Adminstrator tools -> Add ODBC data sources -> MySQL data source 
install.packages(RODBC)
aneurysm is ODBC source 
ocon<- odbcConnect("aneurysm", uid="userone", pwd="*******")
res <- sqlQuery(ocon, "select * from aneurysm")
str(res)


/*=========================================================================
 *
 *  Copyright (c) Karl T. Diedrich 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *=========================================================================*/