#/*=========================================================================
# *
# *  Copyright (c) Karl T. Diedrich 
# *
# *  Licensed under the Apache License, Version 2.0 (the "License");
# *  you may not use this file except in compliance with the License.
# *  You may obtain a copy of the License at
# *
# *         http://www.apache.org/licenses/LICENSE-2.0.txt
# *
# *  Unless required by applicable law or agreed to in writing, software
# *  distributed under the License is distributed on an "AS IS" BASIS,
# *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# *  See the License for the specific language governing permissions and
# *  limitations under the License.
# *
# *=========================================================================*/

# Author Karl T. Diedrich <ktdiedrich@gmail.com>
# this uses RMySQL connection

# Notes on using ODBC and R because RMySQL is not running on 64 bit windows yet 
 # Packages 
 # Connector/ODBC 5.1.4 http://dev.mysql.com/downloads/connector/odbc/5.1.html
 # Control Panel -> Adminstrator tools -> Add ODBC data sources -> MySQL data source 
 # install.packages(RODBC)
 # mysqlaneurysm is ODBC source 
# ocon<- odbcConnect("aneurysm", uid="ktdiedrich", pwd="*******")
# res <- sqlQuery(ocon, "select * from aneurysm")
#  str(res)

STEPS=6000;
VERT_D_UPPER = 30;
BASILAR_D_LOWER = 5; 
BASILAR_D_UPPER = 20;
DISTANCE_SQ <- " and distance > %f and distance < %f ";
MAX_AGE = 1000
RES_DELTA = 0.001;
BASILAR_D_SQ <- sprintf(DISTANCE_SQ,  BASILAR_D_LOWER, BASILAR_D_UPPER);
FROM_LEFT = 5;
FROM_RIGHT = 10000;
CON_TYPE = "RODBC";
SAMPLE = 5;

rc <- colors();
mycolors <- c(rc[24], rc[552], rc[259], rc[26], rc[7], rc[645], rc[598], rc[8], rc[16], rc[12], rc[30], rc[31], rc[32], rc[46], rc[47], 
	rc[51], rc[53], rc[56], rc[62], rc[76], rc[81], rc[84], rc[93], rc[95], rc[100], rc[116], rc[119], rc[120], rc[121], rc[125], rc[133], rc[137], rc[139], rc[142], rc[146], rc[367], rc[371], rc[556], rc[594], rc[576], rc[640], rc[652]
	);

grays <- c("gray20", "gray50", "gray80", "gray10", "gray40", "gray70", "gray100");

dbConn<-function(conType=CON_TYPE, db="aneurysm", pwd="****", uid="*****")
{
	# conType = "RMySQL", "RODBC"
	# uid, pwd needed for conType == "RODBC"
	if (conType == "RMySQL")
	{
		library(RMySQL);
		con <- dbConnect(MySQL(), group=db);
	}
	else if (conType == "RODBC")
	{
		library(RODBC);
		con<- odbcConnect(db, uid=uid, pwd=pwd);
	}
	con;
}

dbClose<-function(con, conType)
{
	if (conType == "RMySQL")
	{
		dbDisconnect(con);
	}
	else if (conType == "RODBC")
	{
		odbcClose(con);
	}
}

dbQuery <- function(con, conType, query)
{
	if (conType == "RMySQL")
	{
		res <- dbGetQuery(con, query);
	}
	else if (conType == "RODBC")
	{
		res <- sqlQuery(con, query);
	}
	res;
}

idListSubQuery <- function(ids)
{
	q <- "";
	idLen = length(ids);
	if (idLen > 0)
	{
		q <- "";
		for (id in ids)
		{
			q = sprintf("%s %d,", q, id);
		}
		q <- substr(q, 0, nchar(q)-1 );
		q <- sprintf(" (%s) ", q);
	}
	q;
}

numList <- function(nums)
{
	len <- length(nums);
	if (len > 0)
	{
		s <- paste(nums[1]);
		if (len > 1)
		{
			for (n in nums[2:len])
			{
				s <- paste(s, ", ", n, sep="");
			}
		}
	}
	s;
}
subjectIdQuery<-function(subjects)
{
	# creates a string for limiting by subject_id
	sq = "";
	subLen = length(subjects);
	if (subLen > 0 )
	{
		sq = " and i.subject_id in (";
		for (s in 1:subLen)
		{
			sq = sprintf("%s %d,", sq, subjects[s] );
			
		}
		sq = substr(sq, 0, nchar(sq)-1);
		sq = sprintf("%s)", sq);
	}
	sq;
}

arteryIdQuery<-function(arteryIds)
{
	sq = "";
	subLen = length(arteryIds);
	if (subLen > 0 )
	{
		sq = " and a.artery_id in (";
		for (s in 1:subLen)
		{
			sq = sprintf("%s %d,", sq, arteryIds[s] );
			
		}
		sq = substr(sq, 0, nchar(sq)-1);
		sq = sprintf("%s)", sq);
	}
	sq;
}

centAlgQuery<-function(ids)
{
	sq = "";
	subLen = length(ids);
	if (subLen > 0 )
	{
		sq = " and c.algorithm_id in (";
		for (s in 1:subLen)
		{
			sq = sprintf("%s %d,", sq, ids[s] );
			
		}
		sq = substr(sq, 0, nchar(sq)-1);
		sq = sprintf("%s)", sq);
	}
	sq;
}

resolutionQ <- function(res)
{
	resWhere <- "";
	if (!is.null(res))
	{
		resLen = length(res);
		if (resLen  >0)
		{
			resWhere <- sprintf(" c.xres between %f and %f ",  res[1] - RES_DELTA, res[1] + RES_DELTA);
			if (resLen > 1)
			{
				for (r in res[2:length(res)])
				{
					resWhere <- sprintf(" %s or c.xres between %f and %f ", resWhere, r - RES_DELTA, r + RES_DELTA);
				}
			}
			resWhere <- sprintf(" and (%s) ", resWhere);
		}
	}
	resWhere;
}

getArteryNameSide <- function(arteryId,  db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich")
{
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	anq <- "select arteryname, shortname, side from arteryview where artery_id = %d ";
	q <- sprintf(anq, arteryId );
	res <- dbQuery(con, conType, q);
	dbClose(con, conType);
	aname <- "";
	if (res$side != 'single')
	{
		aname <- sprintf("%s %s", res$shortname, res$side);
	}
	else
	{
		aname <- res$shortname;
	}
	as.vector(aname);
}

getDiagnosis <- function(diagnosisId,  db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich")
{
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	q <- "select diagnosis from diagnosis where diagnosis_id = %d ";
	q <- sprintf(q, diagnosisId );
	res <- dbQuery(con, conType, q);
	dbClose(con, conType);
	sprintf("%s", res$diagnosis);
}

getType <- function(typeId,  db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich")
{
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	q <- "select type from type where type_id = %d ";
	q <- sprintf(q, typeId );
	res <- dbQuery(con, conType, q);
	dbClose(con, conType);
	sprintf("%s", res$type);
}


getKindredSubjectIds <- function(kindreds, db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich")
{
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	kq <- "select subject_id from lcagenepisubject where kindred in %s ";
	q <- sprintf(kq, idListSubQuery(kindreds) );
	res <- dbQuery(con, conType, q);
	dbClose(con, conType);
	res$subject_id;
}

getImageIds <- function(subjectId,  db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich")
{
		# return list of imageIds for a subjectId 
		con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
		q <- sprintf("select image_id from image where subject_id=%d", subjectId);
		res <- dbQuery(con, conType, q);
		dbClose(con, conType);
		res$image_id;
}

arrangePlots <- function(arteryLen)
{
	if (arteryLen==1)  { par(mfrow=c(1, 1)); }
	else if (arteryLen ==2) { par(mfrow=c(2, 1)); }
	else if (arteryLen == 3) { par(mfrow=c(3, 1));  }
	else if (arteryLen == 4)  { par(mfrow=c(2, 2)); }
	else if (arteryLen == 5 || arteryLen == 6) { par(mfrow=c(3, 2)); }
	else if (arteryLen > 16) { par(mfrow=c(5, 5)); }
	else if (arteryLen > 9) { par(mfrow=c(4, 4)); }
	else if (arteryLen > 6) { par(mfrow=c(3, 3)); }
}

visualTortuosity<-function(rateexperimentIds=c(), subjectIds=c(), kindreds=c(), aneurysm="all", db="aneurysm",  
	conType=CON_TYPE, pwd="****", uid="ktdiedrich", arteryIds=c(1:8) , minAge=0, maxAge=MAX_AGE, sex=NULL) 
{
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	
	subGroup='';
	if (aneurysm == 'yes') subGroup = 'aneurysm';
	if (aneurysm == 'nohist') subGroup = 'no family history of aneurysm';
	if (aneurysm == 'hist') subGroup = 'family history of aneurysm';
	if (aneurysm == 'first') subGroup = 'family history, aneurysm case or first degree relative';
	rateExpIdQ <- "";
	if (length(rateexperimentIds) > 0)
	{
		rateExpIdQ <- paste(" and x.rateexperiment_id in ", idListSubQuery(rateexperimentIds));
	}
	if (!is.null(sex))
	{
		subGroup <- paste(subGroup, sex);
	}
	if (length(kindreds) > 0)
	{
		subGroup <- paste(subGroup, " Kindreds: ");
		for (kin in kindreds)
		{
			subGroup <- paste(subGroup, kin, ", ");
		}
		subjectIds <- c(subjectIds, getKindredSubjectIds(kindreds=kindreds, db=db, conType=conType, pwd=pwd, uid=uid) );
	}		
	subIdQ="";
	if (length(subjectIds))
	{
		subIdQ <- subjectIdQuery(subjectIds);
	}
	ageQ <- sprintf(" and ageatexam is not null and ageatexam >= %d and ageatexam <= %d ", minAge, maxAge );
	sexQ <- "";
	if (!is.null(sex))
	{
		sexQ <- sprintf(" and sex='%s' ", sex);
	}
	ageQ <- sprintf(" and ageatexam is not null and ageatexam >= %d and ageatexam <= %d ", minAge, maxAge );
	arteryIdQ <- arteryIdQuery(arteryIds);
	groupQ <- paste(" ", subIdQ, " ", ageQ, " ", sexQ, " ", arteryIdQ, " ", rateExpIdQ);
	
	baseQ <- "select r.rater, r.rater_id, x.rateexperiment_id, g.subject_id, g.artery_id, g.rating, arteryname, side, sex, ageatexam from rateexperiment x, rating g, rater r, artery a, side s, subject u, image i %s where x.rateexperiment_id=g.rateexperiment_id and x.rater_id=r.rater_id and g.artery_id=a.artery_id and a.side_id=s.side_id and g.subject_id=u.subject_id and u.subject_id=i.subject_id %s";
	
	anFrom <- "";
	anWhere <- "";
	if (aneurysm == "yes")
	{
		anFrom <- " , subjectaneurysm m ";
		anWhere <- " and i.subject_id=m.subject_id and m.aneurysm_id > 0   ";
		
	}
	else if (aneurysm == "hist")
	{
		anFrom <- " , lcagenepisubject epi "
		anWhere <- " and i.subject_id=epi.subject_id and epi.spouse='no' ";
	}
	else if (aneurysm == "first")
	{
		anFrom <- " , lcagenepisubject epi ";
		anWhere <- " and  i.subject_id=epi.subject_id and epi.spouse='no' and (epi.firstdegree='yes' or epi.aneurysmcase='yes') ";
	}
	else if (aneurysm  == "nohist")
	{
		anFrom <- "  , subjecttype st ";
		anWhere <- " and i.subject_id=st.subject_id and st.type_id=1 ";
	}
	buildQ <- sprintf(baseQ, anFrom, anWhere);
	buildQ <- paste(buildQ, groupQ);
	print(buildQ);
	data <- dbQuery(con=con, conType=conType, query=buildQ);
	dbClose(con=con, conType=conType);
	
	raterList <- paste(unique(data$rater), collapse=", " );
	title <- paste("Visual Tortuosity rating, age ", minAge," to ", maxAge, " ", subGroup, sep="");
	title <- paste(title, "\nRaters: ", raterList );
	
	arteryIds  <- unique(data$artery_id);
	arteryLen <- length(arteryIds);
	arrangePlots(arteryLen);
	
	for (arteryId in arteryIds)
	{
		arteryData <- data[data$artery_id==arteryId,];
		meanRating <- mean(arteryData$rating);
		medianRating <- median(arteryData$rating);
		sdRating <- sd(arteryData$rating);
		hist(arteryData$rating, main=paste(title,"\n",arteryData$arteryname[1], " ", arteryData$side[1], "\nN: ", length(arteryData$rating), ", Mean: ", meanRating, ", Median: ", medianRating, ", SD: ",sdRating, sep=""), xlab="rating", ylab="count");
	}
	
}

plotDFM<-function(subjectIds=c(), kindreds=c(), aneurysm="all", xtype="d", ytype="DFM", db="aneurysm", leg=TRUE, forward=TRUE, reverse=FALSE, conType=CON_TYPE, pwd="****", uid="ktdiedrich", leftTortLimit=FROM_LEFT, rightTortLimit=FROM_RIGHT, rightVertTortLimit=VERT_D_UPPER, arteryIds=c(), algIds=c(3), legendx=0.0, legendy=0.9, tortMeasure='peak' , minAge=0, maxAge=MAX_AGE, sex=NULL, cmdline=FALSE, diagnosisIds=c(), typeIds=c(), medFilterSize=NULL, agePlot=FALSE, xres=NULL, histPlot=FALSE, sample=SAMPLE, intTortTest=FALSE, linewidth=1) 
{
	# for ICA stop curves were L/d doubles back identifying the farthest anterior portion of the ICA loop
	# Distance Factor Metric tortuosity curves
	# xtype="L" for length or xtype ="d" for distance
	# ytype="DFM" for Distance Factor Metric or "DFM3" for 3 neighbor smoothed DFM or "DFE" for Distance From Edge 
	# aneurysm all, yes, nohist.  all: any subject, yes: has aneurysm, nohist: no family history of aneurysm, hist: family history of aneurysm, first: first degree relative or high-risk aneurysm case  
	# limit by subject_id subjectIds = c(2,4)
	# tortMeasure, tortuosity measurement point: peak, end 
	#print FALSE put average output text on graph, TRUE print to terminal 
	# author Karl Diedrich
	Dartery <- c(); DsubjectN <- c(); DaveTort <- c(); DSD <- c(); 
	kLen = length(kindreds);
	if (kLen > 0) subjectIds <- c(subjectIds, getKindredSubjectIds(kindreds=kindreds, db=db, conType=conType, pwd=pwd, uid=uid) );
	
	dfmParam = 'DFM';
	if (ytype == 'DFM3') { dfmParam = 'DFM3'};
	if (ytype=='sCAUC') { dfmParam = 'sCAUC' };
	if (ytype=='LsCAUC') { dfmParam = 'LsCAUC' };
	
	algQ <- centAlgQuery(algIds);
	# arteryIdQ <- arteryIdQuery(arteryIds);
	# arteryQ <- "select a.artery_id, a.arteryname, a.side_id, s.side from measurearteryview ma, artery a, side s where ma.artery_id=a.artery_id and a.side_id=s.side_id %s ";
	# q <- sprintf(arteryQ, arteryIdQ);
	# print(q);
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	# artery <- dbQuery(con=con, conType=conType, query=q);
	
	xLabel="End to end Distance (d) (mm)";
	if (xtype=="L")
	{
		xLabel = "Centerline Length (L) (mm)";
	}
	yLabel = ytype; 
	if (ytype == "DFE")
	{
		yLabel = "Distance From Edge (DFE) (mm)";
	}
	
	sexQ <- "";
	if (!is.null(sex))
	{
		sexQ <- sprintf(" and sex='%s' ", sex);
	}
	subIdQ <- subjectIdQuery(subjectIds);
	arteryLen = length(arteryIds);
	
	arrangePlots(arteryLen);
	
	subGroup='';
	if (aneurysm == 'yes') subGroup = 'aneurysm';
	if (aneurysm == 'nohist') subGroup = 'no family history of aneurysm';
	if (aneurysm == 'hist') subGroup = 'family history including aneurysm';
	if (aneurysm == 'histonly') subGroup = 'family history, no aneurysm';
	if (aneurysm == 'first') subGroup = 'family history, aneurysm or first degree';
	if (aneurysm == 'second') subGroup = 'family history, not aneurysm or first degree';
	diagLen <- length(diagnosisIds);
	ss <- "";
	if ( diagLen > 0)
	{
		for (diagId in diagnosisIds)
		{
			diag <- getDiagnosis(diagnosisId=diagId, db=db, conType=conType, uid=uid, pwd=pwd);
			ss <- paste(ss, diag, sep=", ");
		}
		ss <- substr(ss, 2, nchar(ss));
	}
	typeLen <- length(typeIds)
	if (typeLen > 0)
	{
		for (typId in typeIds)
		{
			typ <- getType(typeId=typId, db=db, conType=conType, uid=uid, pwd=pwd);
			ss <- paste(ss, typ, sep=", ");
		}
		ss <- substr(ss, 2, nchar(ss)); 
	}
	gap <- " ";
	if (nchar(subGroup) > 10)
	{
		gap <- "\n";
	}
	subGroup <- sprintf("%s%s%s", subGroup, gap, ss);
	if (!is.null(sex))
	{
		subGroup <- paste(subGroup, sex);
	}
	if (!is.null(medFilterSize))
	{
		subGroup <- paste(subGroup, " MedFilter=", numList(medFilterSize), sep="");
	}
	if (!is.null(xres))
	{
		subGroup <- paste(subGroup, " xres=", numList(xres), sep="");
	}
	if (length(kindreds) > 0)
	{
		subGroup <- sprintf("%s Kindreds: ", subGroup )
		for (kin in kindreds)
		{
			subGroup <- sprintf("%s %s", subGroup, kin);
		}
	}
	
	if (minAge == 0 && maxAge >= MAX_AGE)
	{
		ageLabel <- ""; # "All ages";
	}
	if (minAge > 0 && maxAge < MAX_AGE)
	{
		ageLabel <- sprintf("Age %d to %d", minAge, maxAge);
	}
	if (minAge==0 && maxAge < MAX_AGE)
	{
		ageLabel <- sprintf("Age up to %d", maxAge);
	}
	if (minAge > 0 && maxAge >= MAX_AGE)
	{
		ageLabel <- sprintf("Age %d and up", minAge);
	}
	mainGroup <- sprintf("%s\n%s", ageLabel, subGroup);
	
	allUsubjectIds = c();
	allArteryIds = c();
	allUtorts = c();
	allUages = c();
	
	ageSubjectId <- c();
	ageImageId <- c();
	ageTort <- c();
	ageAge <- c();
	ageArteryId <-c();
		
	for (a in 1:arteryLen)
	{
		artery_id <- arteryIds[a];
		# vertebral arteries have own right limit 
		if (artery_id == 5 || artery_id == 6)
		{
			rightTortLimit = rightVertTortLimit;
		}
		
		arteryname = getArteryNameSide(db=db,conType=conType,uid=uid,pwd=pwd, arteryId=artery_id);
		
		xTypeQ = "distance";
		if (xtype=="L")
		{
			xTypeQ = "length";
		}
		dirQ <- "";
		if (forward == TRUE && reverse == FALSE)
		{
			dirQ <- " and direction='forward' ";
		}
		else if (forward == FALSE && reverse == TRUE)
		{
			dirQ <- " and direction='reverse' "
		}
		limitsQ <- "select max(%s) as max from image i, segmentation g, centerlinetortuosity ct, dfm d, centerline c where i.image_id=g.image_id and g.segmentation_id=c.segmentation_id and ct.usable != 'no' and c.rescorrect!='no' and artery_id in %s and ct.tortuosity_id=d.tortuosity_id and ct.centerline_id=c.centerline_id  %s %s %s";
		if (artery_id != 30 && artery_id != 31)
		{
			arteryIdListQ <- sprintf("(%s)", artery_id);
		}
		if (artery_id == 30)
		{
			arteryIdListQ <- idListSubQuery(c(22, 24, 26, 28));
		}
		if (artery_id == 31)
		{
			arteryIdListQ <- idListSubQuery(c(23, 25, 27, 29));
		}
		q = sprintf(limitsQ, xTypeQ, arteryIdListQ, dirQ, algQ, subIdQ);
		# print(q);
		maxX <- dbQuery(con, conType, q)$max[1];
		
		if (is.na(maxX)) maxX = 20.0;
		
		yTypeQ = "dfm";
		if (ytype == "dfe")
		{
			yTypeQ = "dfe";
		}
		q = sprintf(limitsQ, yTypeQ, arteryIdListQ, dirQ, algQ, subIdQ);
		# print(q);
		maxY <- dbQuery(con, conType, q)$max[1];
		maxY <- maxY + 0.2*maxY;
		if (is.na(maxY)) maxY = 5.0;
		
		
		
		ageQ <- sprintf(" and ageatexam is not null and ageatexam >= %d and ageatexam <= %d ", minAge, maxAge );
		selectQ <- "select distinct ct.tortuosity_id, c.centerline_id, i.subject_id, i.image_id, i.ageatexam, u.subject_id, s.hist2dthres, s.medfiltersize, c.xres, c.yres, c.zres ";
		diagFrom = "";
		diagWhere = "";
		diagSelect = "";
		if (length(diagnosisIds) > 0)
		{
			diagFrom = " , subjectdiagnosis sd";
			diagWhere = paste(" and u.subject_id=sd.subject_id and sd.diagnosis_id in ", idListSubQuery(diagnosisIds) );
			diagSelect = ", diagnosis_id ";
		}
		typeFrom = "";
		typeWhere = "";
		typeSelect = "";
		if (length(typeIds) > 0)
		{
			typeFrom = " , subjecttype st ";
			typeWhere = paste(" and u.subject_id=st.subject_id and st.type_id in ", idListSubQuery(typeIds) );
			typeSelect  = ", type_id ";
		}
		medFilterSizeWhere = "";
		if (!is.null(medFilterSize))
		{
			medFilterSizeWhere = paste(" and medfiltersize in ", idListSubQuery(medFilterSize) );
		}
		
		resWhere <- resolutionQ(res=xres);
		
		groupQ <- sprintf(" %s %s %s %s %s %s %s %s ", subIdQ, algQ, ageQ, sexQ, diagWhere, typeWhere, medFilterSizeWhere, resWhere);
		selectQ <- paste(selectQ, diagSelect, typeSelect, sep="");
		if (aneurysm == "yes")
		{
			q = sprintf("%s from subject u, centerlinetortuosity ct, centerline c, segmentation s, image i, subjectaneurysm m %s %s  where u.subject_id=i.subject_id and ct.artery_id in %s and ct.centerline_id=c.centerline_id and c.segmentation_id=s.segmentation_id and s.image_id=i.image_id and c.rescorrect!='no' and ct.usable!='no' and i.subject_id=m.subject_id and  m.aneurysm_id > 0  %s ", selectQ, diagFrom, typeFrom, arteryIdListQ, groupQ);
		
		}
		else if (aneurysm == "hist")
		{
			q = sprintf("%s from subject u, centerlinetortuosity ct, centerline c, segmentation s, image i, lcagenepisubject epi %s %s where u.subject_id=i.subject_id and ct.artery_id in %s and ct.centerline_id=c.centerline_id and c.segmentation_id=s.segmentation_id and s.image_id=i.image_id and c.rescorrect!='no' and ct.usable!='no' and  i.subject_id=epi.subject_id and epi.spouse='no' %s ", selectQ, diagFrom, typeFrom, arteryIdListQ, groupQ);
		}
		else if (aneurysm == "histonly")
		{
			q = sprintf("%s from subject u, centerlinetortuosity ct, centerline c, segmentation s, image i, lcagenepisubject epi %s %s where epi.aneurysmcase='no' and u.subject_id=i.subject_id and ct.artery_id in %s and ct.centerline_id=c.centerline_id and c.segmentation_id=s.segmentation_id and s.image_id=i.image_id and c.rescorrect!='no' and ct.usable!='no' and  i.subject_id=epi.subject_id and epi.spouse='no' %s ", selectQ, diagFrom, typeFrom, arteryIdListQ, groupQ);
		}
		else if (aneurysm == "first")
		{
			q = sprintf("%s from subject u, centerlinetortuosity ct, centerline c, segmentation s, image i, lcagenepisubject epi %s %s where u.subject_id=i.subject_id and ct.artery_id in %s and ct.centerline_id=c.centerline_id and c.segmentation_id=s.segmentation_id and s.image_id=i.image_id and c.rescorrect!='no' and ct.usable!='no' and  i.subject_id=epi.subject_id and epi.spouse='no' and (epi.firstdegree='yes' or epi.aneurysmcase='yes') %s ", selectQ, diagFrom, typeFrom, arteryIdListQ, groupQ);
		}
		else if (aneurysm == "second")
		{
			q = sprintf("%s from subject u, centerlinetortuosity ct, centerline c, segmentation s, image i, lcagenepisubject epi %s %s where u.subject_id=i.subject_id and ct.artery_id in %s and ct.centerline_id=c.centerline_id and c.segmentation_id=s.segmentation_id and s.image_id=i.image_id and c.rescorrect!='no' and ct.usable!='no' and  i.subject_id=epi.subject_id and epi.spouse='no' and epi.firstdegree='no' and epi.aneurysmcase='no' %s ", selectQ, diagFrom, typeFrom, arteryIdListQ, groupQ);
		}
		else if (aneurysm  == "nohist")
		{
			q= sprintf("%s from subject u, centerlinetortuosity ct, centerline c, segmentation s, image i,  subjecttype st %s %s where u.subject_id=i.subject_id and i.subject_id=st.subject_id and st.type_id=1 and ct.artery_id in %s and ct.centerline_id=c.centerline_id and c.segmentation_id=s.segmentation_id and s.image_id=i.image_id and c.rescorrect!='no' and ct.usable!='no'  %s", selectQ, diagFrom, typeFrom, arteryIdListQ, groupQ);
		}
		else  # aneurysm == 'all' 
		{
			q = sprintf("%s from subject u, centerlinetortuosity ct, centerline c, segmentation s, image i %s %s where u.subject_id=i.subject_id and ct.artery_id in %s and ct.centerline_id=c.centerline_id and c.segmentation_id=s.segmentation_id and s.image_id=i.image_id and c.rescorrect!='no' and ct.usable!='no'  %s ", selectQ, diagFrom, typeFrom, arteryIdListQ, groupQ);
		}
		
		print(q);
		
		tortuosity <- dbQuery(con, conType, q);
		meanXres <- 0; meanYres <- 0; meanZres <- 0;
		if (length(tortuosity$xres) > 0)
		{
			meanXres <- mean(tortuosity$xres);
			meanYres <- mean(tortuosity$yres);
			meanZres <- mean(tortuosity$zres);
		}
		tortuosityLen = length(tortuosity$tortuosity_id);
		lineNum <- length(tortuosity$tortuosity_id);
		labels = c();
		theseColors = c();
		
		if (tortuosityLen > 0)
		{
			lineTypes = rep(1, lineNum);
			fLineType = 1;
			rLineType = 2;
			uSubjects <- unique(tortuosity$subject_id);
			lenSubjects <- length(uSubjects);
			# print(sprintf("Unique subjects: %d", lenSubjects));
			uSubjTorts <- rep(0, lenSubjects);
			uSubjAges <- rep(0, lenSubjects);
			uSubjDFMmeas <- rep(0, lenSubjects);
			
			for (i in 1:lenSubjects) 
			{
				printDFM = FALSE;
				if (lenSubjects < 5) printDFM=TRUE;
				
				ret <- dfmTort(subjectId=uSubjects[i], arteryId=artery_id, db=db, conType=conType, pwd=pwd, uid=uid, leftLimit=leftTortLimit, rightLimit=rightTortLimit, measure=tortMeasure, cmdline=printDFM, dfm=dfmParam, xres=xres, medFilterSize=medFilterSize);
				subImageIds = getImageIds(subjectId=uSubjects[i], db=db, conType=conType, pwd=pwd, uid=uid);
				ageSubjectId <- c(ageSubjectId, rep(uSubjects[i], length(subImageIds)));
				ageArteryId <- c(ageArteryId, rep(artery_id, length(subImageIds)));
				ageImageId <- c(ageImageId, subImageIds);
				for (sid in subImageIds)
				{	
					sidRet <- dfmTort(subjectId=uSubjects[i], imageId=sid, arteryId=artery_id, db=db, conType=conType, pwd=pwd, uid=uid, leftLimit=leftTortLimit, rightLimit=rightTortLimit, measure=tortMeasure, cmdline=FALSE, dfm=dfmParam, xres=xres, medFilterSize=medFilterSize);
					sidTort <- sidRet[1];
					sidAge <- sidRet[2];
					ageTort <- c(ageTort, sidTort);
					ageAge <- c(ageAge, sidAge);
				}
				 uSubjTorts[i] <- ret[1];
				 uSubjAges[i] <- ret[2];
				 uSubjDFMmeas[i] <- ret[3];
			}
			thisMinAge <- min(uSubjAges);
			thisMaxAge <- max(uSubjAges);
			allUsubjectIds <- c(allUsubjectIds, uSubjects);
			allUtorts <- c(allUtorts, uSubjTorts);
			allUages <- c(allUages, uSubjAges);
			allArteryIds <- c(allArteryIds, rep(artery_id, lenSubjects) );
			
			# print("Subjects: "); print(uSubjects); print("DFMs:"); print(uSubjTorts);
			meanTort <- mean(uSubjTorts);
			medianTort <- median(uSubjTorts);
			meanDFMmeas <- mean(uSubjDFMmeas);
			sdTort <- sd(uSubjTorts);
			meanAge <- mean(uSubjAges);
			medianAge <- median(uSubjAges);
			sdAge <- sd(uSubjAges);
			# maxY <- max(allUtorts);
			
			aveLabel <- sprintf("N=%d %s Mean=%.5f Median=%.5f SD=%.5f\nAges %.0f to %.0f Mean=%.5f Median=%.5f SD=%.5f\nMean: Res. X=%.5f Y=%.5f Z=%.5f Measures=%.5f", lenSubjects, tortMeasure, meanTort, medianTort, sdTort, thisMinAge, thisMaxAge, meanAge, medianAge, sdAge, meanXres, meanYres, meanZres, meanDFMmeas);
				Dartery <- c(Dartery, arteryname); 
				DsubjectN <- c(DsubjectN, lenSubjects); 
				DaveTort <- c(DaveTort, meanTort); 
				DSD <- c(DSD, sdTort);
			plot(c(), c(), main=sprintf("%s %s", arteryname, mainGroup), xlab=xLabel, ylab=yLabel, type="l",  lty=1, col=1, xlim=c(0, maxX), ylim=c(0, maxY) );
			abline(v=rightTortLimit, lty=3, col="snow3");
			if (leftTortLimit > 0 && tortMeasure != 'end')
			{
				abline(v=leftTortLimit, lty=3, col="snow3");
			}
			if (cmdline == FALSE)
			{
				text(maxX/2, maxY*.925, labels=aveLabel, pos=1, offset=-.4, cex=0.8);
			}
			else
			{
				print(paste(arteryname, " ", aveLabel, sep=""));
			}
			
			for (t in 1:lineNum)
			{
				if ((t %%length(mycolors)) == 0)
				{
					fLineType = fLineType+1; 
					rLineType = rLineType+1;
				}
				lineTypes[t] = fLineType;
				tortuosity_id = tortuosity$tortuosity_id[t];
				labels <- append(labels, sprintf("s%di%dS%.0fM%dc%dt%d", tortuosity$subject_id[t], tortuosity$image_id[t], tortuosity$hist2dthres[t]*100, tortuosity$medfiltersize[t], tortuosity$centerline_id[t], tortuosity$tortuosity_id[t]) );
				# print(sprintf("tortuosity_id=%d", tortuosity_id))
				q = sprintf("select length as L, distance as d, dfm as DFM, dfe as DFE from dfm where tortuosity_id=%d and direction='forward' ", tortuosity_id);
				# print(q);
				
				# dbdfm <- dbQuery(con, conType, q);
				fdfm <- centerlineDFM(tortuosityId=tortuosity_id, db=db, conType=conType, pwd=pwd);
				intTortSamp <- centerlineDT(fdfm, sample=sample);
				# q = sprintf("select length, distance, dfm, dfe from dfm where tortuosity_id=%d and direction='reverse'", tortuosity_id);
				# print(q);
				
				# rdfm <- dbQuery(con, conType, q);
				# print(sprintf("Max forward DFM: %f, Max reverse DFML: ", max(fdfm$dfm), max(rdfm$dfm)));
				
				thisColor = t;
				if ( t < length(mycolors) )
				{
					thisColor = mycolors[t];
					theseColors <- c(theseColors, thisColor);
				}
				
		
				if      (xtype=="d" & ytype=="DFM") 
				{ 
					if (forward) 
					{ 
						lines(fdfm$d, fdfm$DFM, col=thisColor, lty=fLineType, lwd=linewidth) ;
						# lines(dbdfm$d, dbdfm$DFM, col=rc[47], lty=rLineType) ;
					}
					# if (reverse) lines(rdfm$distance, rdfm$dfm, col=thisColor, lty=rLineType);
				}
				if      (xtype=="d" & ytype=="DFM3") 
				{ 
					if (forward) 
					{ 
						lines(fdfm$d, fdfm$DFM3, col=thisColor, lty=fLineType, lwd=linewidth);
					}
					# if (reverse) lines(rdfm$distance, rdfm$dfm, col=thisColor, lty=rLineType);
				}
				else if (xtype=="L" & ytype=="DFM") 
				{
					if (forward) 
					{
						lines(fdfm$L, fdfm$DFM, col=thisColor, lty=fLineType, lwd=linewidth);
						if (intTortTest)
						{
							lines(intTortSamp$L, intTortSamp$slope, col=thisColor, lty=fLineType+1);
							lines(intTortSamp$L, (intTortSamp$sCAUC), col=thisColor, lty=fLineType+2);
							lines(intTortSamp$L, intTortSamp$Ls, col=thisColor, lty=fLineType+3);
							lines(intTortSamp$L, (intTortSamp$LsCAUC), col=thisColor, lty=fLineType+4);
						}
					}
					# if (reverse)  lines(rdfm$length, rdfm$dfm, col=thisColor, lty=rLineType);
				}
				else if (xtype=="L" & ytype=="DFM3") 
				{
					if (forward) 
					{
						lines(fdfm$L, fdfm$DFM3, col=thisColor, lty=fLineType, lwd=linewidth);
					}
					# if (reverse)  lines(rdfm$length, rdfm$dfm, col=thisColor, lty=rLineType);
				}
				else if (xtype=="d" & ytype=="DFE") 
				{
					if(forward) 
					{
						lines(fdfm$d, fdfm$DFE, col=thisColor, lty=fLineType, lwd=linewidth);
					}
					# if (reverse) lines(rdfm$distance, rdfm$dfe, col=thisColor, lty=rLineType);
				}
				else if (xtype=="L" & ytype=="DFE") 
				{
					if (forward) 
					{
						lines(fdfm$L, fdfm$DFE, col=thisColor, lty=fLineType, lwd=linewidth);
					}
					# if (reverse) lines(rdfm$length, rdfm$dfe, col=thisColor, lty=rLineType);
				}
				else if (xtype=="L" & ytype=="sCAUC") 
				{
					#lines(intTortSamp$L, intTortSamp$slope, col=thisColor, lty=fLineType+1);
					lines(intTortSamp$L, (intTortSamp$sCAUC), col=thisColor, lty=fLineType, lwd=linewidth);
				}
				else if (xtype=="L" & ytype=="LsCAUC") 
				{
					#lines(intTortSamp$L, intTortSamp$Ls, col=thisColor, lty=fLineType+3);
					lines(intTortSamp$L, (intTortSamp$LsCAUC), col=thisColor, lty=fLineType, lwd=linewidth);
				}
				
			}
		}
		if (leg & length(labels) > 0)
		{ 
			legend(maxX*legendx, maxY*legendy, labels, col=theseColors, lty=lineTypes, bty="n");
		}
		
	}
	dbClose(con, conType);
	if (agePlot==TRUE)
	{
		x11();
		arrangePlots(arteryLen);
		aArteryIds = unique(ageArteryId);
		ageData <- data.frame(arteryId = ageArteryId, subjectId = ageSubjectId, age = ageAge, tort = ageTort, imageId = ageImageId);
		ageData <- ageData[ageData$age > 0, ];
		for (aid in aArteryIds)
		{
			data <- ageData[ageData$arteryId==aid, ];
			uAge <- unique(data$age);
			corTitle <- "";
			if (length(uAge) > 1)
			{
				ageCor <- cor(data$age, data$tort);
				corTitle <- sprintf("Cor=%.5f", ageCor);
			}
			aNameSide <- getArteryNameSide(arteryId=aid, db=db, conType=conType, uid=uid, pwd=pwd );
			plot(data$age, data$tort, main=sprintf("%s N=%d %s\n%s", aNameSide, length(data$age), corTitle, subGroup), xlab="Age", ylab=paste(tortMeasure, dfmParam));
		
			text(data$age, data$tort, labels=paste(data$subjectId, data$imageId, sep="-"), pos=1, offset=-.4, cex=0.8);
		
			if ( length(uAge) > 1 )
			{
				abline(lm(data$tort~data$age));
			}
		}
	}
	if (histPlot == TRUE)
	{
		x11();
		arrangePlots(arteryLen);
		for (aid in arteryIds)
		{
			aNameSide = getArteryNameSide(db=db,conType=conType,uid=uid,pwd=pwd, arteryId=aid);
			aTorts <- sort( allUtorts[allArteryIds==aid] );
			meanT <- mean(aTorts);
			minT <- min(aTorts);
			maxT <- max(aTorts);
			rangeT = maxT-minT;
			sdT <- sd(aTorts);
			
			lenT = length(aTorts);
			# print(aTorts); 
			minT <- min(aTorts);
			maxT <- max(aTorts);
			rangeT <- maxT - minT;
			hist(aTorts, breaks=seq(minT, maxT, rangeT/100), xlab=paste(tortMeasure, dfmParam), main=sprintf("%s, %s N=%d\nMean=%.5f SD=%.5f min=%.5f max=%.5f", aNameSide, mainGroup, lenT, meanT, sdT, minT, maxT));
			#dense <- density(aTorts);
			#lines(dense, col=2);
		}
		
		x11();
		arrangePlots(arteryLen);
		for (aid in arteryIds)
		{
			aNameSide = getArteryNameSide(db=db,conType=conType,uid=uid,pwd=pwd, arteryId=aid);
			aTorts <- sort( allUtorts[allArteryIds==aid] );
			normTest <- shapiro.test(aTorts);
			qqnorm(aTorts, main=sprintf("%s, %s\nShaprio normality: %.5f", aNameSide, mainGroup, normTest$p));
			qqline(aTorts, lty=2);
			
		}
	}
	
	data <- data.frame(arteryId = allArteryIds, tortuosity = allUtorts,  subjectId = allUsubjectIds );
	data <- data[order(data$arteryId, -data$tortuosity),]
}

compareDFM <- function(subjectIdsA=c(), kindredsA=c(), aneurysmA="all", minAgeA=0, maxAgeA=MAX_AGE, sexA=NULL, diagnosisIdsA=c(), typeIdsA=c(), subjectIdsB=c(), kindredsB=c(), aneurysmB="all", minAgeB=0, maxAgeB=MAX_AGE,  sexB=NULL, diagnosisIdsB=c(), typeIdsB=c(), xtype="d", ytype="DFM", db="aneurysm", leg=FALSE, forward=TRUE, reverse=FALSE,  conType=CON_TYPE, pwd="****", uid="ktdiedrich", leftTortLimit=FROM_LEFT, rightTortLimit=FROM_RIGHT, rightVertTortLimit=VERT_D_UPPER, arteryIds=c(), algIds=c(3), legendx=0.67, legendy=1, tortMeasure="peak", labelA='A', labelB='B', medFilterSizeA=NULL, medFilterSizeB=NULL, xresA=NULL, xresB=NULL, histPlot=FALSE, agePlot=FALSE, tests=c("T", "F", "W"), testSide="two.sided", notch=F, barLeg="top", meanText=FALSE, linewidth=1)
{
	# tests options: T, F, Wilcoxon
	# testSide options: two.sided, greater, less
	# F test is always two.sided 
	library(MASS);
	dfA <- plotDFM(subjectIds=subjectIdsA, kindreds=kindredsA, aneurysm=aneurysmA, minAge=minAgeA, maxAge=maxAgeA, sex=sexA, diagnosisIds=diagnosisIdsA, typeIds=typeIdsA, xtype=xtype, ytype=ytype, db=db, leg=leg, forward=forward, reverse=reverse, 
		conType=conType, pwd=pwd, uid=uid, leftTortLimit=leftTortLimit, rightTortLimit=rightTortLimit, rightVertTortLimit=rightVertTortLimit, arteryIds=arteryIds, algIds=algIds, legendx=legendx, legendy=legendy, tortMeasure=tortMeasure, medFilterSize=medFilterSizeA, xres=xresA, histPlot=histPlot, agePlot=agePlot, linewidth=linewidth);
	x11();
	dfB <- plotDFM(subjectIds=subjectIdsB, kindreds=kindredsB, aneurysm=aneurysmB, minAge=minAgeB, maxAge=maxAgeB, sex=sexB, diagnosisIds=diagnosisIdsB, typeIds=typeIdsB, xtype=xtype, ytype=ytype, db=db, leg=leg, forward=forward, reverse=reverse, 
		conType=conType, pwd=pwd, uid=uid, leftTortLimit=leftTortLimit, rightTortLimit=rightTortLimit, rightVertTortLimit=rightVertTortLimit, arteryIds=arteryIds, algIds=algIds, legendx=legendx, legendy=legendy, tortMeasure=tortMeasure, medFilterSize=medFilterSizeB, xres=xresB, histPlot=histPlot, agePlot=agePlot, linewidth=linewidth);
		
	if (length(arteryIds) == 0 ) arteryIds = unique(dfA$arteryId);
	arteryLen  = length(arteryIds);
	x11();
	if (arteryLen==1)  par(mfrow=c(1,1));
	if (arteryLen ==2)	 par(mfrow=c(1,2));
	if (arteryLen == 3) par(mfrow=c(1,3)); 
	if (arteryLen == 4)  par(mfrow=c(2,2))
	if (arteryLen == 5 || arteryLen == 6) par(mfrow=c(2,3));
	if (arteryLen > 6) 	par(mfrow=c(3,3));
	testList <- c();
	AB <- merge(dfA, dfB, by=c("arteryId", "subjectId"));
	for (arteryId in arteryIds)
	{
		arteryNameSide <- getArteryNameSide(arteryId=arteryId, db=db, conType=conType, uid=uid, pwd=pwd );
		tortA <- dfA[dfA$arteryId==arteryId, c("tortuosity")];
		adA <- dfA[dfA$arteryId==arteryId, ];
		facA <- rep(labelA, length(tortA));
		
		tortB <- dfB[dfB$arteryId==arteryId, c("tortuosity")];
		adB <- dfB[dfB$arteryId==arteryId, ];
		facB <- rep(labelB, length(tortB));
		
		adAB <- AB[AB$arteryId==arteryId, ];
		lenadAB <- length(adAB$subjectId);
				
		mnta <- mean(tortA);
		mntb <- mean(tortB);
		smaller <- mnta;
		larger <- mntb;
		if (mntb < mnta)
		{
			smaller <- mntb;
			larger <- mnta;
		}
		difference <- larger - smaller;
		increase <- difference / smaller;
		
		artTestList <- c();
		Tlabel <- "";
		Flabel <- "";
		Wlabel <- "";
		print( sprintf( "Tests on artery: %s, Mean difference : %f", getArteryNameSide(arteryId, db=db, conType=conType, pwd=pwd, uid=uid), increase ) );
		for (tst in tests)
		{
			if (tst == "T")
			{
				tRes <- t.test(tortA, tortB, alternative=testSide);
				artTestList <- c(artTestList, tRes);
				Tlabel <- sprintf("T p=%.5f",  tRes$p.value);
				print(tRes);
				if (lenadAB > 0)
				{
					pairT <- t.test(adAB$tortuosity.x, adAB$tortuosity.y, paired=TRUE);
					print(pairT);
					Tlabel <- sprintf("%s, %dpr=%.5f", Tlabel, lenadAB, pairT$p.value);
				}
			}
			if (tst == "F")
			{
				fRes <- var.test(tortA, tortB, alternative="two.sided");
				artTestList <- c(artTestList, fRes);
				Flabel <- sprintf("F p=%.5f", fRes$p.value);
				print(fRes);
			}
			if (tst == "W")
			{
				wRes <- wilcox.test(tortA, tortB, alternative=testSide);
				artTestList <- c(artTestList, wRes);
				Wlabel <- sprintf("Wilcoxon p=%.5f", wRes$p.value);
				print(wRes);
				if (lenadAB > 0)
				{
					pairW <- wilcox.test(adAB$tortuosity.x, adAB$tortuosity.y, paired=TRUE);
					print(pairW);
					Wlabel <- sprintf("%s, %dpr=%.5f", Wlabel, lenadAB, pairW$p.value);
				}
			}
		}
		testList <- c(testList, artTestList);
		
		df <- data.frame(tort=c(tortA, tortB), fac=as.factor(c(facA, facB)));
		gap = " ";
		if (Flabel != "" && Tlabel != "" && Wlabel != "")
		{
			gap="\n";
		}
		plot(df$tort ~ df$fac, notch=notch, main=sprintf("%s %.3f%% increase\n%s %s %s%s%s", arteryNameSide, increase*100, Flabel, testSide, Tlabel, gap, Wlabel), ylab=paste(tortMeasure, ytype), xlab='group');
	}
	
	dfA$type <- rep(labelA, length(dfA$subjectId));
	dfB$type <- rep(labelB, length(dfB$subjectId));
	
	dfA$arteryName <- rep("X", length(dfA$subjectId));
	dfB$arteryName <- rep("X", length(dfB$subjectId));
	lenAB <- length(AB$subjectId);
	if (lenAB > 0) 
	{
		AB$arteryName <- rep("X", length(AB$subjectId));
	}
	arteryNames  <- c();
	for (aid in arteryIds)
	{
		name <- getArteryNameSide(arteryId=aid,  db=db, conType=conType, pwd=pwd, uid=uid);
		arteryNames <- c(arteryNames, name);
		dfA[dfA$arteryId==aid,]$arteryName <- name;
		dfB[dfB$arteryId==aid,]$arteryName <- name;
		if (lenAB > 0)
		{
			AB[AB$arteryId==aid,]$arteryName <- name;
		}
		
	}
	My <- matrix(nrow=2, ncol=length(arteryNames), dimnames = list(c(labelA, labelB), arteryNames) );
	Msd <- matrix(nrow=2, ncol=length(arteryNames), dimnames = list(c(labelA, labelB), arteryNames) );
	
	if (lenAB > 0)
	{
		ABy <- matrix(nrow=2, ncol=length(arteryNames), dimnames = list(c(labelA, labelB), arteryNames) );
		ABsd <- matrix(nrow=2, ncol=length(arteryNames), dimnames = list(c(labelA, labelB), arteryNames) );
	}
	i = 1;
	if (lenAB > 0)
	{
		x11();
		arrangePlots(arteryLen)
		for (an in arteryNames)
		{
			aAB <- AB[AB$arteryName == an,];
			
			# correlation 
			aABcor <- cor(aAB$tortuosity.x, aAB$tortuosity.y);
			plot(aAB$tortuosity.x, aAB$tortuosity.y, main=sprintf("%s %d pairs cor=%.5f", an, length(aAB$subjectId), aABcor), xlab=sprintf("%s %s %s", labelA, tortMeasure, ytype), ylab=sprintf("%s %s %s", labelB, tortMeasure, ytype));
			abline(lm(aAB$tortuosity.y~aAB$tortuosity.x));
		}
	}
	for (an in arteryNames)
	{
		meanA <- mean(dfA[dfA$arteryName==an,]$tortuosity);
		sdA <- sd(dfA[dfA$arteryName==an,]$tortuosity);
		meanB <-  mean(dfB[dfB$arteryName==an,]$tortuosity);
		sdB <- sd(dfB[dfB$arteryName==an,]$tortuosity);
		My[1,i] <- meanA;
		My[2,i] <- meanB;
		Msd[1,i] <- sdA;
		Msd[2,i] <- sdB;
	
		if (lenAB > 0)
		{
			ABmeanA <- mean(AB[AB$arteryName==an,]$tortuosity.x);
			ABmeanB <- mean(AB[AB$arteryName==an,]$tortuosity.y);
			ABsdA <- sd(AB[AB$arteryName==an,]$tortuosity.x);
			ABsdB <- sd(AB[AB$arteryName==an,]$tortuosity.y);
			ABy[1,i] <- ABmeanA;
			ABy[2,i] <- ABmeanB;
			ABsd[1,i] <- ABsdA;
			ABsd[2,i] <- ABsdB;
		}
		i = i+1;
	}
	x11();
	fillColor <- c('gray50', 'gray75');
	Mx <- barplot(My, beside=T, main=sprintf("%s-%s tortuosity", labelA, labelB), xlab="artery", ylab=sprintf("mean %s %s", tortMeasure, ytype), col=fillColor, ylim=c(0, max(My+Msd)));
	lenMy <- length(My);
	diffM <- matrix(nrow=2, ncol=length(arteryNames), dimnames = list(c(labelA, labelB), arteryNames) );
	for (k in 1:(lenMy/2))
	{
		t  = k*2;
		s = t-1;
		a = My[s];
		b = My[t];
		lesser = a;
		if (b < a) { lesser = b; }
		diffM[t] = abs(a-b)/lesser;
		diffM[s] = diffM[t];
	}
	yTextPos <- My/3;
	if (meanText==TRUE)
	{
		text(x=Mx, y=yTextPos, labels=sprintf("%.4f+-\n%.5f\n%.3f%%", My, Msd, diffM*100), cex=0.8);
	}
	legend(barLeg, c(labelA, labelB), fill=fillColor, bty="n");
	# error bars 
	arrows(Mx,My+Msd, Mx, My-Msd, angle=90, code=3, length=0.1);

	if (lenAB > 0)
	{
		x11();
		
		ABx <- barplot(ABy, beside=T, main=sprintf("%s-%s paired tortuosity", labelA, labelB), xlab="artery", ylab=sprintf("mean %s %s", tortMeasure, ytype), col=fillColor, ylim=c(0, max(ABy+ABsd)) ); 
		lenABy <- length(ABy);
		diffAB <- matrix(nrow=2, ncol=length(arteryNames), dimnames = list(c(labelA, labelB), arteryNames) );
		for (k in 1:(lenABy/2))
		{
			t  = k*2;
			s = t-1;
			a = ABy[s];
			b = ABy[t];
			lesser = a;
			if (b < a) { lesser = b; }
			diffAB[t] = abs(a-b)/lesser;
			diffAB[s] = diffAB[t];
		}
		yTextPos <- ABy/3;
		if (meanText==TRUE)
		{
			# text(x=ABx, y=yTextPos, labels=sprintf("%.4f+-\n%.5f\n%.3f%%", ABy, ABsd,diffAB*100), cex=0.8);
		}
		legend(barLeg, c(labelA, labelB), fill=fillColor, bty="n");
		# error bars 
		arrows(ABx,ABy+ABsd, ABx, ABy-ABsd, angle=90, code=3, length=0.1);
	}
	testList;
	# My;
	#ABy;
	# AB;
}

tortuosityAnova <- function(diagnosisIds=c(), typeIds=c(), xres=c(), dfm="DFM", db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich", leftTortLimit=FROM_LEFT, rightTortLimit=FROM_RIGHT, rightVertTortLimit=VERT_D_UPPER, arteryIds=c(), algIds=c(3), tortMeasure='peak', cmdline=FALSE, medFilterSize=NULL, plotTukey=FALSE, notch=FALSE, barLeg="top")
{
	plotsPer = 1;
	if (plotTukey==TRUE)
	{
		plotsPer = 2;
	}
	arteryLen = length(arteryIds);
	typeLen = length(typeIds);
	resLen = length(xres);
	
	diagnosisLen = length(diagnosisIds);
	arteryWhere <- arteryIdQuery(arteryIds);
	algQ <- centAlgQuery(algIds);
	diagFrom = "";
	diagWhere = "";
	diagSelect = "";
	if (diagnosisLen > 0)
	{
		diagFrom = " , subjectdiagnosis sd, diagnosis d ";
		diagWhere = paste(" and u.subject_id=sd.subject_id and sd.diagnosis_id=d.diagnosis_id and sd.diagnosis_id in ", idListSubQuery(diagnosisIds) );
		diagSelect = ", sd.diagnosis_id,  diagnosis ";
	}
	typeFrom = "";
	typeWhere = "";
	typeSelect = "";
	if (typeLen > 0)
	{
		typeFrom = " , subjecttype st, type t ";
		typeWhere = paste(" and u.subject_id=st.subject_id and st.type_id=t.type_id and st.type_id in ", idListSubQuery(typeIds) );
		typeSelect  = ", st.type_id, type ";
	}
	resWhere <- resolutionQ(res=xres);
	medFilterSizeWhere = "";
	medFilterLen = 0;
	if (!is.null(medFilterSize))
	{
		medFilterLen = length(medFilterSize);
		medFilterSizeWhere = paste(" and medfiltersize in ", idListSubQuery(medFilterSize) );
	}
	
	selectQ <- "select distinct ct.tortuosity_id, c.centerline_id, i.subject_id, i.image_id, i.ageatexam, a.artery_id, a.shortname, s.side, g.medfiltersize, c.xres, c.yres, c.zres ";
	selectQ <- paste(selectQ, diagSelect, typeSelect, sep="");
	
	fromQ <- paste(" from subject u, centerlinetortuosity ct, centerline c, segmentation g, image i , artery a, side s ", diagFrom, typeFrom, sep="");
	
	whereQ <- paste(" where ct.artery_id=a.artery_id and a.side_id=s.side_id and u.subject_id=i.subject_id and ct.centerline_id=c.centerline_id and c.segmentation_id=g.segmentation_id and g.image_id=i.image_id and c.rescorrect!='no' and ct.usable!='no'     and c.algorithm_id in ( 3)  and ageatexam is not null and ageatexam >= 0 and ageatexam <= 1000 ", diagWhere, typeWhere, medFilterSizeWhere, arteryWhere, sep="");
	
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	
	query <- paste(selectQ, fromQ, whereQ);
	print(query);
	data <- dbQuery(con, conType, query);
	dbClose(con, conType);
	dataLen <- length(data$tortuosity_id);
	data$tortuosity <- rep(0, dataLen);
	data$meanAge <- rep(0, dataLen);
	for (i in 1:dataLen)
	{
		row <- data[i,];
		if (row$artery_id ==5 || row$artery_id ==6 )
		{
			rightLimit = rightVertTortLimit;
		}
		else 
		{
			rightLimit = rightTortLimit; 
		}
		rowTort <- dfmTort(subjectId=row$subject_id, imageId=row$image_id, arteryId=row$artery_id, db=db, conType=conType, pwd=pwd, uid=uid, leftLimit=leftTortLimit, rightLimit=rightLimit, measure=tortMeasure, cmdline=FALSE, dfm=dfm, xres=xres, medFilterSize=medFilterSize);
		data$tortuosity[i] <- rowTort[1];
		data$meanAge[i] <- rowTort[2];
	}
	data$arteryname <- paste(data$shortname, data$side);
	if (typeLen > 2)
	{
		x11();
		arrangePlots(arteryLen*plotsPer);
		data$typeFactor <- as.factor(data$type);
		for (aid in arteryIds)
		{
			arteryData <- data[data$artery_id==aid,];
			aNameSide <- arteryData$arteryname[1];
		
			factors <- arteryData$typeFactor;
			shfac <- rep("X", length(factors));
			
			shfac <- substr(factors, 1, 1);
			
			types <- as.factor(shfac);
			if (length(unique(factors)) > 1)
			{
				aovRes <- aov(arteryData$tortuosity ~  factors);
				plot(arteryData$tortuosity ~ factors,  main=sprintf("ANOVA of %s tortuosity", aNameSide), ylab=paste(tortMeasure, dfm), xlab="type", notch=notch );
				print(sprintf("%s: ", aNameSide));
				print(summary(aovRes));
				print(TukeyHSD(aovRes));
				if (plotTukey) { plot(TukeyHSD(aov(arteryData$tortuosity ~ types))); }
			}
		}	
		x11();
		My <- tapply(data$tortuosity, list(data$typeFactor, data$arteryname), mean);
		Msd <- tapply(data$tortuosity, list( data$typeFactor, data$arteryname), sd);
		Mx <- barplot(My, beside=T, main=sprintf("Type factors"), xlab="artery", ylab=sprintf("mean %s %s", tortMeasure, dfm), ylim=c(0, max(My+(Msd*typeLen))), col=grays[1:typeLen]);
		arrows(Mx, My+Msd, Mx, My-Msd, angle=90, code=3, length=0.1);
		legend(x="top", legend=sort(unique(data$typeFactor)), bty='n', fill=grays[1:typeLen]);
	}
	
	if (resLen > 2)
	{
		x11();
		arrangePlots(arteryLen*plotsPer);
		data$resFactor <- as.factor(sprintf("%.2f", data$xres));
		for (aid in arteryIds)
		{
			arteryData <- data[data$artery_id==aid,];
			aNameSide <- paste(arteryData$arteryname[1], arteryData$side[1]);
			
			if (length(unique(arteryData$resFactor)) > 1)
			{
				resolution <- arteryData$resFactor;
				aovRes <- aov(arteryData$tortuosity ~  resolution );
				plot(arteryData$tortuosity ~ resolution,  main=sprintf("ANOVA of %s tortuosity", aNameSide), ylab=paste(dfm," tortuosity L/d"), xlab="X resolution", notch=notch );
				print(sprintf("%s: ", aNameSide));
				print(summary(aovRes));
				print(TukeyHSD(aovRes));
				if (plotTukey) { plot(TukeyHSD(aovRes)); }
			}
		}	
		x11();
		My <- tapply(data$tortuosity, list(data$resFactor, data$arteryname), mean);
		Msd <- tapply(data$tortuosity, list( data$resFactor, data$arteryname), sd);
		Mx <- barplot(My, beside=T, main=sprintf("Resolution factors"), xlab="artery", ylab=sprintf("mean %s %s", tortMeasure, dfm), ylim=c(0, max(My+(Msd*resLen))), col=grays[1:resLen]);
		arrows(Mx, My+Msd, Mx, My-Msd, angle=90, code=3, length=0.1);
		legend(x="top", legend=sort(unique(data$resFactor)), bty='n', fill=grays[1:resLen]);
	}
	if (diagnosisLen > 2)
	{
		x11();
		arrangePlots(arteryLen*plotsPer);
		data$diagnosisFactor <- as.factor(data$diagnosis);
		for (aid in arteryIds)
		{
			arteryData <- data[data$artery_id==aid,];
			aNameSide <- paste(arteryData$arteryname[1], arteryData$side[1]);
		
			factors <- arteryData$diagnosisFactor;
			shfac <- rep("X", length(factors));
			
			shfac <- substr(factors, 1, 1);
			
			diagnosises <- as.factor(shfac);
			if (length(unique(factors)) > 1)
			{
				aovRes <- aov(arteryData$tortuosity ~  factors);
				plot(arteryData$tortuosity ~ factors,  main=sprintf("ANOVA of %s tortuosity", aNameSide), ylab=paste(dfm," tortuosity L/d"), xlab="diagnosis", notch=notch );
				print(sprintf("%s: ", aNameSide));
				print(summary(aovRes));
				print(TukeyHSD(aovRes));
				if (plotTukey) { plot(TukeyHSD(aov(arteryData$tortuosity ~ diagnosises))); }
			}
		}
		x11();
		My <- tapply(data$tortuosity, list(data$diagnosisFactor, data$arteryname), mean);
		Msd <- tapply(data$tortuosity, list( data$diagnosisFactor, data$arteryname), sd);
		Mx <- barplot(My, beside=T, main=sprintf("Diagnosis factors"), xlab="artery", ylab=sprintf("mean %s %s", tortMeasure, dfm), ylim=c(0, max(My+(Msd*diagnosisLen))), col=grays[1:diagnosisLen]);
		arrows(Mx, My+Msd, Mx, My-Msd, angle=90, code=3, length=0.1);
		legend(x="top", legend=sort(unique(data$diagnosisFactor)), bty='n', fill=grays[1:diagnosisLen]);
	}
	if (medFilterLen > 2)
	{
		x11();
		arrangePlots(arteryLen*plotsPer);
		data$filterFactor <- as.factor(data$medfiltersize);
		for (aid in arteryIds)
		{
			arteryData <- data[data$artery_id==aid,];
			aNameSide <- paste(arteryData$arteryname[1], arteryData$side[1]);
		
			factors <- arteryData$filterFactor;
			if (length(unique(factors)) > 1)
			{
				aovRes <- aov(arteryData$tortuosity ~  factors);
				plot(arteryData$tortuosity ~ factors,  main=sprintf("ANOVA of %s tortuosity", aNameSide), ylab=paste(dfm," tortuosity L/d"), xlab="median filter size", notch=notch );
				print(sprintf("%s: ", aNameSide));
				print(summary(aovRes));
				print(TukeyHSD(aovRes));
				if (plotTukey) { plot(TukeyHSD(aovRes)); }
			}
		}
		x11();
		My <- tapply(data$tortuosity, list(data$filterFactor, data$arteryname), mean);
		Msd <- tapply(data$tortuosity, list( data$filterFactor, data$arteryname), sd);
		Mx <- barplot(My, beside=T, main=sprintf("Median filter size factors"), xlab="artery", ylab=sprintf("mean %s %s", tortMeasure, dfm), ylim=c(0, max(My+(Msd*medFilterLen))), col=grays[1:medFilterLen]);
		arrows(Mx, My+Msd, Mx, My-Msd, angle=90, code=3, length=0.1);
		legend(x="top", legend=sort(unique(data$filterFactor)), bty='n', fill=grays[1:medFilterLen]);
	}
	data;
}

graphSubjectArtery <- function(db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich")
{
#arteryshape_id | arteryshape  |
#              1 | present      | 
#              2 | absent       | 
#              3 | short        | 
#              4 | narrow       | 
#              5 | thick        | 
#              6 | short narrow | 
#              7 | short thick  | 
#              8 | unknown  
	
	
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	
	# don't select Posterior Cerebral Arteries, images didn't have the resolution to view 
	q = "select va.artery_id, a.arteryname, s.side from variableartery va, artery a, side s where va.artery_id = a.artery_id and a.side_id = s.side_id and va.artery_id  not in(10,11)";
	# print(q);
	
	varArt <- dbQuery(con=con, conType=conType, query=q);
	varArtLen = length(varArt$artery_id);
	present <- rep(0, varArtLen);
	absent <- rep(0, varArtLen);
	total <- rep(0, varArtLen);
	presentFraction <- rep(0, varArtLen);
	
	x <- 1:varArtLen;
	for (i in x)
	{
		# present in some shape 
		q = sprintf("select count(*) as rows from subjectartery where artery_id=%d and arteryshape_id not in (2,8) ", varArt$artery_id[i]);
		
		shapeRows <- dbQuery(con=con, conType=conType, query=q);
		present[i] = shapeRows$rows[1];
		# absent 
		q = sprintf("select count(*) as rows from subjectartery where artery_id=%d and arteryshape_id = 2 ", varArt$artery_id[i]);
		shapeRows = dbQuery(con, conType, q);
		shapeRows <- dbQuery(con=con, conType=conType, query=q);
		absent[i] = shapeRows$rows[1];
		total[i] = present[i] + absent[i];
		presentFraction[i] = present[i] / total[i];
	}
	
	dbClose(con, conType);
	names <- paste(varArt$arteryname, varArt$side);
	names <- paste(total, names);
	x11();
	pLen <- length(names);
	clrs <- 1:pLen; 
	# plot(x, presentFraction, main="Artery present in subject", xlab="Subject count & Artery", ylab="Present in fraction of subjects", axes=FALSE);
	barplot(presentFraction, legend=names, col=clrs, main="Artery present in subject",  xlab="Subject count & Artery", ylab="Present in fraction of subjects", axes=TRUE);
	
	#axis(1, at=x, lab=names);
	y <- seq(0, 1, 0.01);
	axis(2, at=y, lab=y);
}

visualVsDFM <- function(db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich")
{
	# compare the visual ICA tortuosity with the quantitative DFM scores
	# library(RMySQL)
	# con = dbConnect(MySQL(), group=db)
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	
	q = "select distinct subject_id from visualicameasurement";
	# subjects = dbGetQuery(con, q);
	subjects <- dbQuery(con=con, conType=conType, query=q);
	subLen = length(subjects$subject_id);
	s <- 1:subLen;
	visualICAsums <- rep(0, subLen);
	leftICApeaks <- rep(0, subLen);
	rightICApeaks <- rep(0, subLen);
	q = "select rater_id, rater from rater order by rater_id";
	# raters <- dbGetQuery(con, q);
	raters <- dbQuery(con=con, conType=conType, query=q);
	raterLen <- length(raters$rater);
	ratings <- rep(0, subLen*raterLen);
	dim(ratings) = c(subLen, raterLen);
	for (i in s)
	{
		# compare ICA sums to visual scores of each rater
		qv = sprintf("select subject_id, v.rater_id, rater, score from visualicameasurement v, rater r where v.rater_id=r.rater_id and subject_id=%d ", subjects$subject_id[i]);
		
		vs <- dbQuery(con=con, conType=conType, query=qv);
		q = sprintf("select sum(score) as sum from visualicameasurement where subject_id=%d ", subjects$subject_id[i]);
		
		ss <- dbQuery(con=con, conType=conType, query=q);
		visualICAsums[i] <- ss$sum[1];
		qs <- "select max(d.dfm) as maxdfm from image i, segmentation g, centerline c, centerlinetortuosity t, dfm d where i.subject_id=%d and i.image_id=g.image_id and g.segmentation_id=c.segmentation_id and c.centerline_id=t.centerline_id and c.rescorrect!='no' and t.usable != 'no' and t.artery_id = %d and t.tortuosity_id=d.tortuosity_id";
		# left ICA
		q = sprintf(qs, subjects$subject_id[i], 1);
		
		left <- dbQuery(con=con, conType=conType, query=q);
		leftICApeaks[i] <- left$maxdfm[1];
		# right ICA
		q = sprintf(qs, subjects$subject_id[i], 2);
		
		right <- dbQuery(con=con, conType=conType, query=q);
		rightICApeaks[i] <- right$maxdfm[1];
        
		qr <- sprintf("select v.subject_id, r.rater_id, r.rater, v.score from rater r, visualicameasurement v where r.rater_id=v.rater_id and subject_id=%d order by rater_id", subjects$subject_id[i]);
		
		subRatings <- dbQuery(con=con, conType=conType, query=qr);
		for (r in raters$rater_id)
		{
			# TODO: Error in ratings[i, r] <- subRatings$score[r] :  number of items to replace is not a multiple of replacement length
			ratings[i, r] <- subRatings$score[r];
		}
	}
	
	df <- data.frame(subject_id = subjects$subject_id, visualICAsums, leftICApeaks, rightICApeaks);
	leftDf <- df[!is.na(df$leftICApeaks), ];
	rightDf <- df[!is.na(df$rightICApeaks), ];
	leftRatings <- ratings[!is.na(df$leftICApeaks), ];
	rightRatings <- ratings[!is.na(df$rightICApeaks), ];
	leftSumCor <- cor(leftDf$visualICAsums, leftDf$leftICApeaks);
	rightSumCor <- cor(rightDf$visualICAsums, rightDf$rightICApeaks);
	x11();
	par(mfrow=c(2,2));
	plot(leftDf$visualICAsums, leftDf$leftICApeaks, main=sprintf("Left ICA tortuosity cor=%f", leftSumCor), xlab="Visual rating sum", ylab="Peak DFM");
	abline(lm(leftDf$leftICApeaks~leftDf$visualICAsums));
    
	plot(rightDf$visualICAsums, rightDf$rightICApeaks, main=sprintf("Right ICA tortuosity cor=%f", rightSumCor), xlab="Visual rating sum", ylab="Peak DFM");
	abline(lm(rightDf$rightICApeaks~rightDf$visualICAsums));
	
	
	lfVisualICAsums <- c (leftDf$visualICAsums, rightDf$visualICAsums);
	lfICApeaks <- c(leftDf$leftICApeaks, rightDf$rightICApeaks);
	lfSumCor <- cor(lfVisualICAsums, lfICApeaks);
	plot(lfVisualICAsums, lfICApeaks, main=sprintf("ICA Tortuosity cor=%f", lfSumCor), xlab="Visual rating sum", ylab="Peak DFM");
	abline(lm(lfICApeaks~lfVisualICAsums));
    
	x11();
	par(mfrow=c(4,4));
	for (rid in raters$rater_id)
	{
		leftCor <- cor(leftRatings[,rid], leftDf$leftICApeaks);
		rightCor <- cor(rightRatings[, rid], rightDf$rightICApeaks);
		plot(leftRatings[,rid], leftDf$leftICApeaks, main=sprintf("Left ICA %s vs DFM cor=%f", raters$rater[rid], leftCor), xlab="Visual score", ylab="Peak DFM");
		abline(lm(leftDf$leftICApeaks~leftRatings[,rid]));
		plot(rightRatings[,rid], rightDf$rightICApeaks, main=sprintf("Right ICA %s vs DFM cor=%f", raters$rater[rid], rightCor), xlab="Visual score", ylab="Peak DFM");
		abline(lm(rightDf$rightICApeaks~rightRatings[,rid]));
	}
	
	x <- 1:raterLen;
	rs <- rep(0, )
	raterScores<-list();
	for (i in x)
	{
		q = sprintf("select subject_id, score, rater_id from visualicameasurement where rater_id=%d order by subject_id", raters$rater_id[i]);
		# scores <- dbGetQuery(con, q);
		scores <- dbQuery(con=con, conType=conType, query=q);
		raterScores[[i]] <- scores$score;
	}
	r2 = raterLen*raterLen;
	#interRaterCors <- rep(0, r2);
	#interRaterLabels <- rep("x", r2);
	#interRaterColors <- 1:(r2-raterLen);
	
	
	
	for (i in x)
	{
		interRaterCors <- rep(0, raterLen);
		interRaterLabels <- rep("x", raterLen);
		interRaterColors <- 1:(raterLen);
		x11();
		for (j in x)
		{
			#if (i != j)
			
				#index = (i-1)*raterLen + j;
				index = j;
				
				interRaterCor <- cor(raterScores[[i]], raterScores[[j]]);
				interRaterCors[index] <- interRaterCor;
				out <- sprintf("%d: %s to %s: %f", index, raters$rater[i], raters$rater[j], interRaterCor);
				interRaterLabels[index] <- out;
				print(out);
				barplot(interRaterCors, legend=interRaterLabels, col=interRaterColors, main="Correlations between raters",  ylab="Correlation", xlab="Raters", axes=TRUE);
		}
		
	}
	#ircs <- interRaterCors[interRaterCors !=0];
	#irls <- interRaterLabels[interRaterLabels !="x"];
	#x11();
	#barplot(ircs, legend=irls, col=interRaterColors, main="Correlations between raters",  xlab="Correlation", ylab="Raters", axes=TRUE);
	
	
	dbClose(con, conType);
	list(leftDf, rightDf);
}

accumCentHist<-function(filename, pngfile="no")
{
	# histogram read from the centerline stability output file 
	histData<-read.table(filename, header=T);
	allSum <- sum(histData$count);
	stableCount <- max(histData$count);
	unstableSum<-sum(histData$count[histData$count != stableCount]);
	allStablePercent <- stableCount/allSum * 100;
	mLabel <- sprintf("Accumulated centerline stability: percent perfectly stable = %f", (allStablePercent) );
	
	if (pngfile=="no")
	{
		
	}
	else
	{
		png(pngfile);
	}
	
	barplot(histData$count, main=mLabel, xlab="Number of graphs from different start points", ylab="number of voxels");
	axis(1, at=histData$overwrite, lab=histData$overwrite);
	# text(x=histData$overwrite, y=histData$count, labels=histData$count);
	if (pngfile!="no")
	{
		dev.off()
	}
}


dfmTort <- function(subjectId, arteryId, imageId=0, centerlineId=0, tortuosityId=0, db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich", algIds=c(3), leftLimit=FROM_LEFT, rightLimit=FROM_RIGHT, measure="peak", cmdline=FALSE, dfm='DFM', xres=NULL, medFilterSize=NULL, sample=SAMPLE)
{
	# Calculates peak or end tortuosity for the average of the centerlinetortuosity measurements specified.
	# measure = ['peak', 'end']
	# dfm = ['DFM', 'DFM3']
	imageIdQ <- "";
	centerlineIdQ <- "";
	tortuosityIdQ <- "";
	if (arteryId != 30 && arteryId != 31)
	{
		arteryIdListQ <- sprintf("(%s)", arteryId);
	}
	if (arteryId == 30)
	{
		arteryIdListQ <- idListSubQuery(c(22, 24, 26, 28));
	}
	if (arteryId == 31)
	{
		arteryIdListQ <- idListSubQuery(c(23, 25, 27, 29));
	}
	
	if (imageId > 0) imageIdQ <- sprintf(" and i.image_id=%d ", imageId);
	if (centerlineId > 0) centerlineIdQ <- sprintf(" and c.centerline_id=%d ", centerlineId);
	if (tortuosityId > 0) tortuosityIdQ <- sprintf(" and t.tortuosity_id=%d ", tortuosityId);
	algQ <- centAlgQuery(algIds);
	resQ <- resolutionQ(xres);
	medFilterSizeWhere = "";
	if (!is.null(medFilterSize))
	{
		medFilterSizeWhere = paste(" and medfiltersize in ", idListSubQuery(medFilterSize) );
	}
	
	partQ <- sprintf(" %s %s %s %s %s %s ", imageIdQ, centerlineIdQ, tortuosityIdQ, algQ, resQ, medFilterSizeWhere);
	
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	
	fromQ <- sprintf(" from image i, segmentation g, centerline c, centerlinetortuosity t where t.artery_id in %s and i.subject_id=%d and i.image_id=g.image_id and g.segmentation_id=c.segmentation_id and c.centerline_id=t.centerline_id and c.rescorrect!='no' and t.usable != 'no' %s", arteryIdListQ, subjectId,  partQ);
	
	
	entryQ = sprintf("select distinct i.subject_id, i.image_id, c.centerline_id, t.tortuosity_id, t.artery_id, i.ageatexam, c.xres, c.yres, c.zres, g.medfiltersize %s", fromQ);
	# TODO 
	# print(entryQ);
	
	entryRes <- dbQuery(con, conType, entryQ);
	dbClose(con, conType);
	
	entryLen <- length(entryRes$tortuosity_id);
	meanTort <- 0; 
	medianTort <- 0;
	sdTort <- 0;
	meanAge <- 0;
	meadianAge <- 0;
	sdAge <- 0;
	
	if (entryLen > 0)
	{
		torts <- rep(0, entryLen);
		for (i in 1:entryLen)
		{
			tortId <- entryRes$tortuosity_id[i];
			dfmRes <- centerlineDFM(tortuosityId=tortId, db=db, conType=conType, pwd=pwd);
			if (dfm == 'sCAUC' || dfm == 'LsCAUC')
			{
				intTort <- centerlineDT(cent=dfmRes, sample=sample);
			}
			
			if (measure=='peak')
			{
				dfmRes <- dfmRes[dfmRes$d >= leftLimit,];
				dfmRes <- dfmRes[dfmRes$d <= rightLimit,];
				if (dfm == 'DFM') { torts[i] = max(dfmRes$DFM); }
				else if (dfm == 'DFM3') { torts[i] = max(dfmRes$DFM3); }
			}
			else if (measure == 'end')
			{
				len <- length(dfmRes$DFM);
				if (dfm == 'DFM') { torts[i] = dfmRes$DFM[len]; }
				else if (dfm == 'DFM3') { torts[i] = dfmRes$DFM3[len]; }
				else if (dfm == 'sCAUC') { torts[i] = intTort$sCAUC[length(intTort$sCAUC)]; }
				else if (dfm == 'LsCAUC') { torts[i] = intTort$LsCAUC[length(intTort$LsCAUC)]; };
			}
		}
		# print("DFM: "); print(torts);
	
		
		meanTort = mean(torts); 
		medianTort = median(torts);
		sdTort = sd(torts);
		meanAge = mean(entryRes$ageatexam);
		medianAge = median(entryRes$ageatexam);
		sdAge = sd(entryRes$ageatexam);
		if (cmdline ==TRUE)
		{
			arteryname <- getArteryNameSide(arteryId=arteryId, db=db, conType=conType, pwd=pwd);
			print(sprintf("SubjectID: %d, %s %s %s  from %.2f to %.2f, mean: %f, median: %f, SD: %f. Age mean: %f, median: %f, SD: %f", subjectId, arteryname, measure, dfm, leftLimit, rightLimit, meanTort, medianTort, sdTort, meanAge, medianAge, sdAge));
		}
	}
	c(meanTort, meanAge, entryLen);
}

ageAtExam <- function(subjectId, imageId, db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich")
{
	ageQ <- "select i.image_id, i.subject_id, sex, birthdate, examdate, DATE_FORMAT(FROM_DAYS(TO_DAYS(examdate)-TO_DAYS(birthdate)), '%Y')+0 AS age from image i, subject s where i.subject_id=s.subject_id and examdate < '2020' and examdate > '0000-00-00' and birthdate > '0000-00-00' ";
	q <- paste(ageQ, " and s.subject_id=", subjectId, " and image_id=", imageId);
	
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	
	ret <- dbQuery(con, conType, q);
	dbClose(con, conType);
	ret$age[1];
}

aneurysmComp<-function(db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich")
{
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	
	noAnQ="select distinct(subject_id) from subjectaneurysm where aneurysm_id=0";
	noAnSubjects <- dbQuery(con=con, conType=conType, query=noAnQ);
	
	anQ="select distinct(subject_id) from subjectaneurysm where aneurysm_id>0";
	anSubjects <- dbQuery(con=con, conType=conType, query=anQ);
	
	dbClose(con, conType);
	
}


plotArtery<- function(db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich", arteryId, subjectIds=c(), kindreds=c())
{
	library(rgl);
	
	kLen = length(kindreds);
	if (kLen > 0)
	{
		subjectIds <- c(subjectIds, getKindredSubjectIds(kindreds=kindreds, db=db, conType=conType, pwd=pwd, uid=uid) );
	}
	
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	
	tCoordQ <- "select tc.x, tc.y, tc.z, tc.tortuosity_id, ct.centerline_id, ct.artery_id, a.arteryname, s.side, i.subject_id, i.image_id, c.xres, c.yres, c.zres from tortuositycoordinate tc, centerlinetortuosity ct, artery a, side s, centerline c, segmentation g, image i where ct.usable != 'no' and c.rescorrect != 'no' and tc.tortuosity_id=ct.tortuosity_id and ct.artery_id=a.artery_id and a.side_id = s.side_id and ct.centerline_id=c.centerline_id and c.segmentation_id = g.segmentation_id and g.image_id=i.image_id and a.artery_id=%d %s";
	
	subQ <- subjectIdQuery(subjectIds);
	q <- sprintf(tCoordQ, arteryId, subQ);
	# print(q);
	coord <- dbQuery(con=con, conType=conType, query=q);
	dbClose(con, conType);
	coord$xr <- coord$x * coord$xres;
	coord$yr <- coord$y * coord$yres;
	coord$zr <- coord$z * coord$zres;
	
	
	minX <- 999999;
	maxX <- 0;
	
	minY <- 999999;
	maxY <- 0;
	
	minZ <- 999999;
	maxZ <- 0;
	
	tortIds = unique(coord$tortuosity_id);
	xrs <- c();
	yrs <- c();
	zrs <- c();
	for (tortId in tortIds)
	{
		imCoord <- coord[coord$tortuosity_id==tortId, ];
		imLen <- length(imCoord$image_id);
		#baseX<- imCoord$xr[imLen];
		#baseY <- imCoord$yr[imLen];
		#baseZ <- imCoord$zr[imLen];
		baseX <- imCoord$xr[1];
		baseY <- imCoord$yr[1];
		baseZ <- imCoord$zr[1];
		
	
		imCoord$xrs <- imCoord$xr - baseX;
		imCoord$yrs <- imCoord$yr - baseY;
		imCoord$zrs <- imCoord$zr - baseZ;
		xrs <- c(xrs, imCoord$xrs);
		yrs <- c(yrs, imCoord$yrs);
		zrs <- c(zrs, imCoord$zrs);
		
		mnx <- min(imCoord$xrs);
		if (mnx < minX) {minX <- mnx; }
		mxx <- max(imCoord$xrs);
		if (mxx > maxX) {maxX <- mxx; }
		mny <- min(imCoord$yrs);
		if (mny < minY) {minY <- mny; }
		mxy <- max(imCoord$yrs);
		if (mxy > maxY) {maxY <- mxy; }
		mnz <- min(imCoord$zrs);
		if (mnz < minZ) {minZ <- mnz; }
		mxz <- max(imCoord$zrs);
		if (mxz > maxZ) {maxZ <- mxz; }
	}
	coord$xrs <- xrs;
	coord$yrs <- yrs;
	coord$zrs <- zrs;
	maxZRS <- max(coord$zrs);
	coord$zrs <- maxZRS-coord$zrs;
	
	tortId <- tortIds[1];
	
	imCoord <- coord[coord$tortuosity_id == tortId, ];
	imLen <- length(imCoord$image_id);
	
	title = sprintf("%s %s", imCoord$arteryname[1], imCoord$side[1]);
	plotColor = 1;
	
	plot3d(imCoord$xrs, imCoord$yrs, imCoord$zrs, main=title, type="l", col=plotColor, xlab="x", ylab="y", zlab="z", xlim=c(minX, maxX), ylim=c(minY, maxY), zlim=c(minZ, maxZ));
	
	text3d(imCoord$xrs[imLen], imCoord$yrs[imLen], imCoord$zrs[imLen], sprintf("s%di%d", imCoord$subject_id[1], imCoord$image_id[1]), pos=3, cex=0.8, offset=1);
	
	idLen <- length(tortIds);
	if (idLen > 1)
	{
		for (tortId in tortIds[2:idLen])
		{
			plotColor <- plotColor+1;
			#print(imageId);
			imCoord <- coord[coord$tortuosity_id == tortId, ];
			imLen <- length(imCoord$image_id);
			
			lines3d(imCoord$xrs, imCoord$yrs, imCoord$zrs, col=plotColor);
			
			text3d(imCoord$xrs[imLen], imCoord$yrs[imLen], imCoord$zrs[imLen], sprintf("s%di%d", imCoord$subject_id[1], imCoord$image_id[1]), pos=3, cex=0.8, offset=1);
		}
	}
	coord;
}


costHist <- function(data, subjectId, bins = 250)
{
	par(mfrow=c(3,1));
	common <- "used in Dijkstra's algorithm";
	
	maxR = max(data$weight);
	minR = min(data$weight);
	rng = maxR  - minR;
	hist(data$weight, main=sprintf("Subject %d DFE weighted COM costs %s, max=%f", subjectId, common, max(data$weight)),
		breaks=seq(minR, maxR, rng/bins));
	
	if (sum(data$pccost) > 0 )
	{
		
		logData = log10(data$pccost+.001);
		maxR = max(logData);
		minR = min(logData);
		rng = maxR  - minR;
		hist(logData, main=sprintf("Log Subject %d Phase Contrast velocity cost %s, max=%f", subjectId, common, max(data$pccost)), 
			breaks=seq(minR, maxR, rng/bins) );
	}
	maxR = max(data$path);
	minR = min(data$path);
	rng = maxR  - minR;
	hist(data$path, main=sprintf("Subject %d Dijkstra's algorithm costs, max=%f", subjectId, max(data$path)),
		breaks=seq(minR, maxR, rng/bins) );
	
}


loadkindred <- function(kind, db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich")
{
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	
	ksql <- paste("update  lcagenepisubject set kindred=", kind$kind1, ", kindredid=", kind$kid1, " where gid=", kind$gid)
	for (k in ksql)
	{
		# RMySQL code 
		dbSendQuery(con, k);
	}
	
	dbClose(con, conType);
}

correlateTortuosity <- function(rateexperimentId, arteryIds=c(), ratescaleId=1, db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich", leftTortLimit=FROM_LEFT, rightTortLimit=FROM_RIGHT, rightVertTortLimit=VERT_D_UPPER, tortMeasure="peak", incTraining=FALSE, dfm='DFM')
{
	# correlate quantitative and visual tortuosity scores not including training set 
	arteryIdsQ <- "";
	arteryIdLen = length(arteryIds);
	if (arteryIdLen > 0)
	{
		arteryIdsQ <- arteryIdQuery(arteryIds);
	}
	vizTortQ <- sprintf("select r.rater, r.rater_id, e.rateexperiment_id, e.expdate, g.subject_id, g.artery_id, a.arteryname, g.rating from rater r, rateexperiment e, rating g, ratescale c, artery a  where r.rater_id=e.rater_id and e.ratescale_id=c.ratescale_id and g.artery_id=a.artery_id and e.rateexperiment_id=g.rateexperiment_id and e.rateexperiment_id=%d %s", rateexperimentId, arteryIdsQ);
	trainingQ <- "select subject_id as trainingsubject_id, artery_id as trainingartery_id from subjectarterytype where type_id=2";
	print(vizTortQ);
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	vizTort <- dbQuery(con=con, conType=conType, query=vizTortQ);
	trainingSet <- dbQuery(con=con, conType=conType, query=trainingQ);
	if (incTraining==FALSE)
	{
		for (i in 1:length(trainingSet$trainingsubject_id))
		{
			tsid = trainingSet$trainingsubject_id[i];
			taid = trainingSet$trainingartery_id[i];
			print(paste("Remove subject_id ", tsid, " artery_id ", taid));
			vizTort<-vizTort[!(vizTort$subject_id==tsid & vizTort$artery_id==taid),]
		}
	}
	
	arterynames <- unique(vizTort$arteryname);
	nameStr="";
	for (a in arterynames)
	{
		nameStr <- sprintf("%s%s, ", nameStr, a);
	}
	vizLen <- length(vizTort$rating);
	vizTort$dfm <- rep(0, vizLen);
	for (i in 1:vizLen)
	{
		# vertebral arteries right peak tortuosity limit 
		rightLimit = rightTortLimit; 
		if (vizTort$artery_id[i] == 5 || vizTort$artery_id[i] == 6)
		{
			rightLimit = rightVertTortLimit;
		}
		vizTort$dfm[i] <- dfmTort(vizTort$subject_id[i], vizTort$artery_id[i], db=db, conType=conType, uid=uid, pwd=pwd, leftLimit=leftTortLimit, rightLimit=rightLimit, measure=tortMeasure, dfm=dfm)[1]; 
	}
	dbClose(con, conType);
	rater <- vizTort$rater[1];
	expdate <- vizTort$expdate[1];
	vizDFMcor <- cor(vizTort$rating, vizTort$dfm);
	title1 <- sprintf("Visual vs DFM tortuosity, rate experiment: %d, N: %d, rater: %s, date: %s, correlation: %f", rateexperimentId, vizLen, rater, expdate, vizDFMcor);
	title2 <- sprintf("%s tortuosity limits [%f, %f] vertebral [%f, %f]", tortMeasure, leftTortLimit, rightTortLimit, leftTortLimit, rightVertTortLimit);
	title <- c(title1, title2, nameStr);
	
	plot(vizTort$rating, vizTort$dfm, main=title, xlab="visual rating", ylab="DFM");
	abline(lm(vizTort$dfm~vizTort$rating));
	vizTort;
}

corrTrainingSetTort <- function(rateexperimentId, arteryIds=c(), ratescaleId=1, db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich")
{
	# correlate the training set tortuosity with visual tortuisty training experiment
	q <- sprintf("select r.rater, r.rater_id, e.rateexperiment_id, e.expdate, g.subject_id, g.artery_id, a.arteryname, g.rating, sat.value from rater r, rateexperiment e, rating g, ratescale c, artery a, subjectarterytypeview sat  where sat.type_id=2 and sat.subject_id=g.subject_id and sat.artery_id=g.artery_id and r.rater_id=e.rater_id and e.ratescale_id=c.ratescale_id and g.artery_id=a.artery_id and e.rateexperiment_id=%d ", rateexperimentId);
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	res <- dbQuery(con=con, conType=conType, query=q);
	N = length(res$value);
	ratingCor = cor(res$value, res$rating);
	title <- sprintf("Visual tortuosity. User %s, N=%d, correlation=%f date: %s", res$rater[1], N, ratingCor, res$expdate[1]);
	
	plot(res$value, res$rating, main=title, ylab="experiment rating", xlab="training set rating")
	abline(lm(res$rating~res$value));
	dbClose(con, conType);
}

kindredTortANOVA <- function(kindreds=c(), arteryIds=c(1:8), db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich", algIds=c(3), leftTortLimit=FROM_LEFT, rightTortLimit=FROM_RIGHT, rightVertTortLimit=VERT_D_UPPER, tortMeasure="peak", dfm='DFM')
{
	# ANOVA of kindred tortuosities
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	if ( length(kindreds)==0 )
	{
		kindreds <- dbQuery(con=con, conType=conType, query="select distinct kindred from lcagenepisubject ")$kindred;
	}
	allKindred <- c();
	allSubjectId <- c();
	allTort <- c();
	allArteryId <- c();
	allArteryName <- c();
	for (kid in kindreds)
	{
		kSubjectIds = getKindredSubjectIds(kindreds=c(kid), conType=conType, db=db, pwd=pwd, uid=uid);
		for (sid in kSubjectIds)
		{
			for (aid in arteryIds)
			{
				arteryName <- getArteryNameSide(db=db, uid=uid, conType=conType, pwd=pwd, arteryId=aid);
				
				if (aid == 5 || aid == 6)
				{
					rightTortLimit = rightVertTortLimit;
				}
				tort <- dfmTort(subjectId=sid, arteryId=aid, db=db, conType=conType, pwd=pwd, uid=uid, algIds=algIds, leftLimit=leftTortLimit, rightLimit=rightTortLimit, measure=tortMeasure, dfm=dfm)[1];
				if (tort > 0)
				{
					allTort <- c(allTort, tort);
					allKindred <- c(allKindred, kid);
					allSubjectId <- c(allSubjectId, sid);
					allArteryId <- c(allArteryId, aid);
					allArteryName <- c(allArteryName, arteryName);
				}
			}
		}
	}
	dbClose(con, conType);
	df <- data.frame(kindred=allKindred, subjectId=allSubjectId, arteryId=allArteryId, arteryName=allArteryName, tortuosity=allTort);
	
	x11();
	uKindreds <- sort(unique(df$kindred));
	kindredLen <- length(unique(df$kindred));
	df$kindredFactor <- as.factor(df$kindred);
	
	My <- tapply(df$tortuosity, list(df$kindredFactor, df$arteryName), mean);
	Msd <- tapply(df$tortuosity, list(df$kindredFactor, df$arteryName), sd);
	
	Mx <- barplot(My, beside=T, main=sprintf("Kindred factors"),  ylab=sprintf("mean %s %s", tortMeasure, dfm), col=rainbow(kindredLen));
	text(x=Mx, y=0.4, labels=uKindreds, cex=0.8, srt=90);
	arrows(Mx, My+Msd, Mx, My-Msd, angle=90, code=3, length=0.02);
	
	arteryLen = length(arteryIds);
	x11();
	arrangePlots(arteryLen);
	for (aid in arteryIds)
	{
		arteryNameSide <- getArteryNameSide(arteryId=aid, db=db, uid=uid, conType=conType, pwd=pwd);
		arteryDf <-  df[df$arteryId==aid,];
		fKindred <- as.factor(arteryDf$kindred);
		plot(arteryDf$tortuosity ~ fKindred,  main=sprintf("ANOVA of %s tortuosity", arteryNameSide), ylab=paste(dfm," tortuosity L/d"), xlab="kindred" );
		aovRes <- aov(arteryDf$tortuosity ~ fKindred);
		print(sprintf("%s: ", arteryNameSide));
		print(summary(aovRes));
		# print(TukeyHSD(aovRes));
	}
	df;
}

loadAgeAtExam <- function(db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich")
{
	# load image.ageatexam from birthdate and examdate
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	idQ <- "select subject_id, image_id from image where ageatexam is NULL"
	idRet <- dbQuery(con=con, conType=conType, query=idQ);
	loadQ <- "update image set ageatexam=%d where subject_id=%d and image_id=%d";
	
	for (i in 1:length(idRet$subject_id))
	{
		sid = idRet$subject_id[i];
		iid = idRet$image_id[i];
		age <- ageAtExam(subjectId=sid, imageId=iid, db=db, conType=conType, pwd=pwd, uid=uid);
		if (!is.null(age))
		{
			q <- sprintf(loadQ, age, sid, iid);
			#TODO RMySQL only 
			dbSendQuery(con, q);
		}
	}
	dbClose(con, conType);
}



acaSim <- function(xres=0.2, yres=0.2, zres=0.5, length=20, distance=17, scaleY=0.5, scaleZ=0.25, steps=STEPS, shortTitle=FALSE, legendx=.75, legendy=.75, noiseSD=0)
{
	arterySim(start=0.5*pi, end=1.5*pi, arteryname="ACA", xres=xres, yres=yres, zres=zres, distance=distance, length=length, scaleY=scaleY, scaleZ=scaleZ, steps=steps, shortTitle=shortTitle, legendx=legendx, legendy=legendy, noiseSD=noiseSD);	  
}

icaSim <- function(xres=0.2, yres=0.2, zres=0.5, length=50, distance=12, scaleY=3.5, scaleZ=1, steps=STEPS, shortTitle=FALSE, legendx=0, legendy=0.9, noiseSD=0)
{
	arterySim(start=0.5*pi, end=2.5*pi, arteryname="ICA", xres=xres, yres=yres, zres=zres, distance=distance, length=length, scaleY=scaleY, scaleZ=scaleZ, steps=steps, shortTitle=shortTitle, legendx=legendx, legendy=legendy, noiseSD=noiseSD);
}

arterySim <- function(start, end, arteryname, length, distance, xres=0.2, yres=0.2, zres=0.5, scaleY=1, scaleZ=1, steps=STEPS, shortTitle=FALSE, legendx=0.5, legendy=0.5, noiseSD=0)
{
	library(rgl);
	
	radDis = end - start;
	disFac = distance / radDis;
	
	xRndFac = (1/xres);
	x <- seq(start, end, radDis/steps);
	highResLen <- length(x);
	y <- (scaleY * (sin(x)+1)) * disFac;
	x <- (x-x[1])  * disFac;
	z <- scaleZ*x;
	
	
	rdata <-roundArtery(x=x, y=y, z=z, xres=xres, yres=yres, zres=zres, noiseSD=noiseSD);
	
	ux <- unique(rdata$xrnd);
	lux <- length(ux);
	uy <- unique(rdata$yrnd);
	luy <- length(uy);
	uz <- unique(rdata$zrnd);
	luz <- length(uz);
	
	dfm <- measureDFM(rdata$xrnd, rdata$yrnd, rdata$zrnd);
	lenDFM <- length(dfm$DFM) ;
	DFM <- dfm$DFM[lenDFM];
	DFM3 <- dfm$DFM3[lenDFM];
	L <- dfm$L[lenDFM];
	L3 <- dfm$L3[lenDFM];
	d <- dfm$d[lenDFM];
	
	dfmTitle <- sprintf(" L: %.5f L3: %.5f d: %.5f end DFM: %.5f, end DFM3: %.5f", L, L3, d, DFM, DFM3);
	sTitle <- paste("3D Simulated ", arteryname, " ", xres," X ", yres, " X ", zres, " mm", sep="");
	if (noiseSD > 0)
	{
		sTtitle <- paste(sTitle, ", Noise SD: ", noiseSD, sep="");
	}
	
	longTitle <- paste("3D Simulated ", arteryname, " Res X ", xres,  " Y ", yres, " Z ", zres, " Unique X ", lux, " Y ", luy, " Z ", luz,  dfmTitle, ", High res steps ", steps, " Noise SD: ", noiseSD, sep="");
	print(longTitle);
	if (shortTitle==FALSE)
	{
		title=longTitle;
	}
	else 
	{
		title = sTitle;
	}
	df <- data.frame(x=x, xrnd=rdata$xrnd, y=y, yrnd=rdata$yrnd,  z=z, zrnd=rdata$zrnd, L=dfm$L, L3=dfm$L3, d=dfm$d, DFM=dfm$DFM, DFM3=dfm$DFM3);
	ip=0;
	rndI <- c();
	is <- 1:(lenDFM-1) ;
	#print(is);
	for (i in is)
	{
		if (df$d[i] != df$d[i+1])
		{
			rndI <- c(rndI, i);
		}
	}
	df <- df[rndI,];
	plot3d(x, y, z, type='l', col=3, lty=3, xlab="x mm", ylab="y mm", zlab="z mm", main=title);
	lines3d(df$xrnd, df$yrnd, df$zrnd, col=1, lty=4);
	
	plot(df$d, df$DFM, main=title, xlab ="d mm", ylab="DFM/DFM3", type='l', lty=1, col=1);
	lines(df$d, df$DFM3, lty=2, col=2);
	maxDFMY <- max(df$DFM);
	maxDFMX <- max(df$d);
	legend(maxDFMX*legendx, maxDFMY*legendy, c('DFM', 'DFM3'), col=c(1,2), lty=c(1,2), bty="n");
	
	df;
}

roundArtery <- function(x, y, z, xres, yres, zres, noiseSD=0)
{
	xRndFac = 1/xres;
	yRndFac = 1/yres;
	zRndFac = 1/zres;
	
	xrnd <- round(x*xRndFac) / xRndFac;
	yrnd <- round( y*yRndFac) / yRndFac;
	zrnd <- round(z*zRndFac) / zRndFac;
	rndLen <- length(xrnd);
	if (noiseSD > 0)
	{
		# xrnd <- xrnd + round( rnorm(rndLen, mean=0, sd=noiseSD) * xRndFac) / xRndFac;
		#xrnd <- sort(xrnd);
		yrnd <- yrnd + round( rnorm(rndLen, mean=0, sd=noiseSD) * yRndFac) / yRndFac;
		#yrnd <- sort(yrnd);
		zrnd <- zrnd + round( rnorm(rndLen, mean=0, sd=noiseSD) * zRndFac / zRndFac);
		#zrnd <- sort(zrnd);
	}
	
	ux <- unique(xrnd);
	uy <- unique(yrnd);
	uz <- unique(zrnd);
	#print(paste("X resolution ", xres, ", unique X: ", length(ux), sep="" ));
	#print(ux);
	#print(paste("Y resolution ",yres, ", unique Y: ", length(uy), sep="" ));
	#print(uy);
	#print(paste("Z resolution ",zres, ", unique Z: ", length(uz), sep="" ));
	#print(uz);
	data.frame(x=x, y=y, z=z, xrnd=xrnd, yrnd=yrnd, zrnd=zrnd);
}

measureDFM <- function(xrnd, yrnd, zrnd)
{	
	len <- length(xrnd); 
	Lsum = 0;
	L3sum <- 0;
	L <- rep(0, len);
	L3 <- rep(0, len);
	d <- rep(0, len);
	DFM <- rep(0, len);
	DFM3 <- rep(0, len);
	
	sx <- xrnd[1];
	sy <- yrnd[1];
	sz <- zrnd[1];
	
	i = 1;
	ix <- xrnd[i];
	ix3 <- (xrnd[i]+xrnd[i+1]) / 2;
	
	iy <- yrnd[i];
	iy3 <- (yrnd[i]+yrnd[i+1]) / 2;
		
	iz <- zrnd[i];
	iz3 <- (zrnd[i]+zrnd[i+1]) / 2;
		
	ip1x <- xrnd[i+1];
	ip1x3 <- (xrnd[i]+xrnd[i+1]+xrnd[i+2]) / 3;
		
	ip1y <- yrnd[i+1];
	ip1y3 <- (yrnd[i]+yrnd[i+1]+yrnd[i+2]) / 3;
		
	ip1z <- zrnd[i+1];
	ip1z3 <- (zrnd[i]+zrnd[i+1]+zrnd[i+2]) / 3;
		
	mx <- (ip1x - ix);
	my <- (ip1y - iy);
	mz <- (ip1z - iz);
		
	mx3 <- ip1x3 - ix3;
	my3 <- ip1y3 - iy3;
	mz3 <- ip1z3 - iz3;
		
	m <- sqrt(mx*mx + my*my + mz*mz);
	m3 <- sqrt(mx3*mx3 + my3*my3 + mz3*mz3);
		
		
	Lsum <- Lsum+m;
	L3sum <- L3sum + m3;
	L[i+1] <- Lsum;
	L3[i+1] <- L3sum;
	ex <- xrnd[i+1];
	ey <- yrnd[i+1];
	ez <- zrnd[i+1];
		
	dx <- (ex - sx);
	dy <- (ey - sy);
	dz <- (ez - sz);
	dm <- sqrt(dx*dx + dy*dy + dz*dz);
	if (dm > 0)
	{
		d[i+1] <- dm;
		DFM[i+1] <- Lsum/dm;
		DFM3[i+1] <- L3sum/dm;
	}
	
	for (i in 2:(len-2))
	{
		ix <- xrnd[i];
		ix3 <- (xrnd[i-1]+xrnd[i]+xrnd[i+1]) / 3;
		
		iy <- yrnd[i];
		iy3 <- (yrnd[i-1]+yrnd[i]+yrnd[i+1]) / 3;
		
		iz <- zrnd[i];
		iz3 <- (zrnd[i-1]+zrnd[i]+zrnd[i+1]) / 3;
		
		ip1x <- xrnd[i+1];
		ip1x3 <- (xrnd[i]+xrnd[i+1]+xrnd[i+2]) / 3;
		
		ip1y <- yrnd[i+1];
		ip1y3 <- (yrnd[i]+yrnd[i+1]+yrnd[i+2]) / 3;
		
		ip1z <- zrnd[i+1];
		ip1z3 <- (zrnd[i]+zrnd[i+1]+zrnd[i+2]) / 3;
		
		mx <- (ip1x - ix);
		my <- (ip1y - iy);
		mz <- (ip1z - iz);
		
		mx3 <- ip1x3 - ix3;
		my3 <- ip1y3 - iy3;
		mz3 <- ip1z3 - iz3;
		
		m <- sqrt(mx*mx + my*my + mz*mz);
		m3 <- sqrt(mx3*mx3 + my3*my3 + mz3*mz3);
		
		
		Lsum <- Lsum+m;
		L3sum <- L3sum + m3;
		L[i+1] <- Lsum;
		L3[i+1] <- L3sum;
		ex <- xrnd[i+1];
		ey <- yrnd[i+1];
		ez <- zrnd[i+1];
		
		dx <- (ex - sx);
		dy <- (ey - sy);
		dz <- (ez - sz);
		dm <- sqrt(dx*dx + dy*dy + dz*dz);
		if (dm > 0)
		{
			d[i+1] <- dm;
			DFM[i+1] <- Lsum/dm;
			DFM3[i+1] <- L3sum/dm;
		}
	}
	i <- len-1;
	ix <- xrnd[i];
	ix3 <- (xrnd[i-1]+xrnd[i]+xrnd[i+1]) / 3;
		
	iy <- yrnd[i];
	iy3 <- (yrnd[i-1]+yrnd[i]+yrnd[i+1]) / 3;
		
	iz <- zrnd[i];
	iz3 <- (zrnd[i-1]+zrnd[i]+zrnd[i+1]) / 3;
		
	ip1x <- xrnd[i+1];
	ip1x3 <- (xrnd[i]+xrnd[i+1]) / 2;
		
	ip1y <- yrnd[i+1];
	ip1y3 <- (yrnd[i]+yrnd[i+1]) / 2;
		
	ip1z <- zrnd[i+1];
	ip1z3 <- (zrnd[i]+zrnd[i+1]) / 2;
		
	mx <- (ip1x - ix);
	my <- (ip1y - iy);
	mz <- (ip1z - iz);
		
	mx3 <- ip1x3 - ix3;
	my3 <- ip1y3 - iy3;
	mz3 <- ip1z3 - iz3;
		
	m <- sqrt(mx*mx + my*my + mz*mz);
	m3 <- sqrt(mx3*mx3 + my3*my3 + mz3*mz3);
		
		
	Lsum <- Lsum+m;
	L3sum <- L3sum + m3;
	L[i+1] <- Lsum;
	L3[i+1] <- L3sum;
	ex <- xrnd[i+1];
	ey <- yrnd[i+1];
	ez <- zrnd[i+1];
		
	dx <- (ex - sx);
	dy <- (ey - sy);
	dz <- (ez - sz);
	dm <- sqrt(dx*dx + dy*dy + dz*dz);
	if (dm > 0)
	{
		d[i+1] <- dm;
		DFM[i+1] <- Lsum/dm;
		DFM3[i+1] <- L3sum/dm;
	}
	
	data.frame(L=L, L3=L3, d=d, DFM=DFM, DFM3=DFM3);
}

getCenterline <- function(tortuosityId, db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich")
{
	con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	q <- sprintf("select dfe from dfm where tortuosity_id=%d and direction='forward'", tortuosityId);
	rdfe <- dbQuery(con=con, conType=conType, query=q);
	
	q <- sprintf("select c.centerline_id, tortuosity_id, xres, yres, zres from centerline c, centerlinetortuosity t where c.centerline_id=t.centerline_id and tortuosity_id=%d", tortuosityId);
	ret <- dbQuery(con=con, conType=conType, query=q);
	xres <- ret$xres[1];
	yres <- ret$yres[1];
	zres <- ret$zres[1];

	q <- sprintf("select x, y, z from tortuositycoordinate where tortuosity_id=%d", tortuosityId);
	
	ret <- dbQuery(con=con, conType=conType, query=q);
	dbClose(con, conType);
	ret$xr <- ret$x * xres;
	ret$yr <- ret$y * yres;
	ret$zr <- ret$z * zres;
	ret$DFE <- rdfe$dfe;
	ret;
}

centerlineDFM <- function(tortuosityId, db="aneurysm", conType=CON_TYPE, pwd="****", uid="ktdiedrich")
{
	# measure the DFM of a centerline out of the database 
	ret <- getCenterline(tortuosityId=tortuosityId, conType=conType, pwd=pwd, uid=uid);
	dfm <- measureDFM(ret$xr, ret$yr, ret$zr);
	data.frame(x=ret$x, xr=ret$xr, y=ret$y, yr=ret$yr, z=ret$z, zr=ret$zr, L=dfm$L, L3=dfm$L3, d=dfm$d, DFM=dfm$DFM, DFM3=dfm$DFM3, DFE=ret$DFE);
}

centerlineDT <- function(cent, sample=5)
{	
	cent$i <- 1:length(cent$x);
	samp <- cent[cent$i%%sample==0,];
	samp$slope <- slope3D(samp$xr, samp$yr, samp$zr);
	#samp$sCAUC <- cumAUC(samp$L, samp$slope)/max(samp$L);
	samp$sCAUC <- log(cumAUC(samp$L, samp$slope));
	
	samp$Ls <- sqrt(slope2D(samp$L, samp$slope)^2);
	
	#samp$LsCAUC <- cumAUC(samp$L, samp$Ls)/max(samp$L);
	samp$LsCAUC <- log(cumAUC(samp$L, samp$Ls));
	samp;
}


#newimageroot <- function(db="aneurysm", conType=CON_TYPE, pwd='***', uid='ktdiedrich')
#{
	# TODO search and replace, don't use substring 
	#con <- dbConn(conType=conType, db=db, pwd=pwd, uid=uid);
	#q = "select image_id, directory from image";
	#ret <- dbQuery(con=con, conType=conType, query=q);
	#ret$directory <- as.vector(ret$directory);
	#ret$directory <- substring(ret$directory, 21);
	#for (i in 1:length(ret$image_id))
	#{
		#u <- sprintf("update image set directory='%s' where image_id=%d", ret$directory[i], ret$image_id[i]);
		# dbQuery(con=con, conType=conType, query=u);
	#}
	#dbClose(con, conType);
	#ret;
#}

statArteryTort <- function(data, arteryId)
{
	ad <- data[data$arteryId==arteryId,];
	tmean <- mean(ad$tortuosity);
	tmedian <- median(ad$tortuosity);
	tsd <- sd(ad$tortuosity);
	m2sd <- tmean + (2*tsd);
	s <- sprintf("Mean: %f  Median: %f  SD: %f  Mean+2SD: %f", tmean, tmedian, tsd, m2sd);
	print(s);
	ad;
}

ciLines <- function(model)
{
	# from Crawley, M.J., 2007. The R Book 1st ed., Wiley. pg 414
	abline(model);
	M <- model[[12]][2];
	xm <- mean(M);
	n <- length(model[[12]][[2]]);
	ssx <- sum(M^2) - sum(M)^2/n;
	s.t <- qt(0.975, (n-2));
	xv <- seq(min(M), max(M), (max(M)-min(M))/100);
	yv <- coef(model)[1] + coef(model)[2]*xv;
	se <- sqrt(summary(model)[[6]]^2 * (1/n+(xv-xm)^2/ssx));
	ci <- s.t * se;
	uyv <- yv + ci;
	lyv <- yv - ci;
	lines(xv, uyv, lty=2);
	lines(xv, lyv, lty=2);
}

errorBars <- function(yv, z)
{
	# from Crawley, M.J., 2007. The R Book 1st ed., Wiley. pg 56
	xv <- barplot(yv, ylim=c(0, (max(yv)+max(z)) ), names=nn, ylab=deparse(substitute(yv)) );
	g = (max(xv)-min(xv))/50;
	for (i in 1:length(xv))
	{
		lines(c(xv[i],xv[i]), c(yv[i]+z[i], yv[i]-z[i])  );
		lines(c(xv[i]-g, xv[i]+g), c(yv[i]+z[i], yv[i]+z[i]));
		lines(c(xv[i]-g, xv[i]+g), c(yv[i]-z[i], yv[i]-z[i]));
	}
}

error.bar <- function(x, y, upper, lower=upper, length=0.1)
{
      if(length(x) != length(y) | length(y) !=length(lower) | length(lower) != length(upper))
      stop("vectors must be same length")
      arrows(x,y+upper, x, y-lower, angle=90, code=3, length=length)
}

skew <- function(x)
{
	m3 <- sum( (x-mean(x))^3/length(x) );
	s3 <- sqrt(var(x))^3;
	m3/s3;
}

mykurtosis <- function(x)
{
	# kurtosis
	m4 <- sum((x-mean(x))^4)/length(x);
	s4 <- var(x)^2;
	m4/s4 - 3;
}

slope2D <- function(x, y)
{
	x1 <- x[2:length(x)]
	y1 <- y[2:length(y)]
	yd <- y1-y[1:(length(y)-1)];
	xd <- x1-x[1:(length(x)-1)];
	s <- c(0, yd/xd);
}

slope3D <- function(x, y, z, zero2min=TRUE)
{
	FAC = 1;
	x1 <- x[2:length(x)];
	y1 <- y[2:length(y)];
	z1 <- z[2:length(z)];
	
	xd <- x1-x[1:(length(x)-1)];
	yd <- y1-y[1:(length(y)-1)];
	zd <- z1-z[1:(length(z)-1)];
	if (zero2min==TRUE)
	{
		mx <- min(abs(xd[xd!=0]));
		my <- min(abs(yd[yd!=0]));
		mz <- min(abs(zd[zd!=0]));
		xd[xd==0] <- mx/FAC;
		yd[yd==0] <- my/FAC;
		zd[zd==0] <- mz/FAC;
	}
	
	syx <- yd/xd;
	szx <- zd/xd;
	s <- c(0, sqrt(syx^2 + szx^2) )
}

cumAUC <- function(x, y)
{
	x1 <- x[2:length(x)];
	y1 <- y[2:length(y)];
	xd <- x1-x[1:(length(x)-1)];
	
	a <- c(0, xd*y1);
	ca <- rep(0, length(a));
	for (i in 2: length(a))
	{
		ca[i] = ca[i-1]+a[i];
	}
	ca;
}

AUC <- function(x,y)
{
	ca <- cumAUC(x,y);
	ca[length(ca)];
}

