--/*=========================================================================
-- *
-- *  Copyright (c) Karl T. Diedrich 
-- *
-- *  Licensed under the Apache License, Version 2.0 (the "License");
-- *  you may not use this file except in compliance with the License.
-- *  You may obtain a copy of the License at
-- *
-- *         http://www.apache.org/licenses/LICENSE-2.0.txt
-- *
-- *  Unless required by applicable law or agreed to in writing, software
-- *  distributed under the License is distributed on an "AS IS" BASIS,
-- *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- *  See the License for the specific language governing permissions and
-- *  limitations under the License.
-- *
-- *=========================================================================*/

-- create tables for storing ImageJ Aneurysm plugin data
-- especially the tortuosity measurement data
-- @author Karl.Diedrich@utah.edu

-- metadata tables 
create table side
(
    side_id int not null,
    side varchar(20) not null,
    primary key (side_id)
);

create table brand
(
    brand_id int not null,
    brand varchar(20) not null,
    primary key(brand_id)
);

create table mri
(
    mri_id int not null,
    tesla float not null,
    brand_id int not null references brand(brand_id),
    primary key(mri_id)
);

create table artery
(
    artery_id int not null,
    arteryname varchar(256)  not null,
    side_id int not null default 0 references side(side_id),
    primary key(artery_id)
);

-- 0 no aneurysm
-- >= 1 aneurysm 
create table aneurysm
(
    aneurysm_id int not null,
    shape varchar(256) not null,
    artery_id int not null references artery(artery_id), 
    location varchar(256) not null,
    primary key(aneurysm_id)
);

-- running data 
create table subject
(
    subject_id int not null, -- study id 
    birthdate date null,
    sex enum('male', 'female', 'unknown') not null,
    visualicatortuosity int null, 
    note varchar(256) null,
    primary key(subject_id)
);

create table subjectaneurysm
(
	subject_id int not null references subject(subject_id),
	aneurysm_id int not null references aneurysm(aneurysm_id),
	diameter float null,
	note varchar(256) null
);

create table image
(
    image_id int not null auto_increment,
    filename varchar(256) not null,
    directory varchar(256) not null,
    mri_id int not null references mri(mri_id),
    xres float not null,
    yres float not null,
    zres float not null, 
    subject_id int not null references subject(subject_id), 
    examdate date null, 
    note varchar(256) null,
    changedimage varchar(255) null,
    primary key(image_id)
);

-- segmentation parameters used
create table segmentation 
(
    segmentation_id int not null  auto_increment,
    min2dseed int not null,
    min3dcluster int not null,
    maxchisqrsmooth double not null,
    voxelzdiff int not null,
    hist2dthres double not null,
    scalpskull int not null,
    hollfillit int not null,
    holefillthres int not null,
    holefillneighborhood int not null,
    medfiltersize int not null, -- 0 for no median filter
    medfilterstdevabove double not null, -- default 0
    image_id int not null references image(image_id), 
    primary key(segmentation_id)
);

-- centerline parameters used 
create table centerline
(
    centerline_id int not null  auto_increment,
    dfethreshold float not null,
    minlinelength int not null,
    segmentation_id int null references segmentation(segmentation_id),
    weighta float not null,
    weightb float not null, 
    xres float null,
    yres float null,
    zres float null, 
    rescorrect enum("yes", "no", "unknown") default "unknown" not null, 
    primary key(centerline_id)
);

create table centerlinetortuosity
(
    tortuosity_id int not null  auto_increment,
    artery_id int not null references artery(artery_id),
    centerline_id int not null references centerline(centerline_id),  -- tortuosity measured from a centerline 
    startx int not null,
    starty int not null,
    startz int not null,
    endx int not null,
    endy int not null,
    endz int not null,
    usable enum('yes', 'no', 'unknown') not null default 'unknown',
    primary key(tortuosity_id)
);

create table dfm
(
    tortuosity_id int not null references centerlinetortuosity(tortuosity_id),
    length float not null,
    distance float not null,
    dfm float not null,
    dfe float not null, -- centerline.defthreshold does not affect this measure 
    direction enum('forward', 'reverse') not null default 'forward'
);

create table arteryshape
(
	arteryshape_id int not null,
	arteryshape varchar(255) not null,
	primary key(arteryshape_id)
);

create table subjectartery
(
	subject_id int not null references subject(subject_id),
	artery_id int not null not null references artery(artery_id),
	arteryshape_id int not null references arteryshape(arteryshape_id),
	primary key(subject_id, artery_id, arteryshape_id)
);

create table rater
(
	rater_id int not null,
	rater varchar(255) not null,
	primary key(rater_id)
)  ENGINE=InnoDB ;

create table visualicameasurement (
  subject_id int NOT NULL references subject(subject_id),
  score int not null,
  rater_id int not null references rater(rater_id), 
  PRIMARY KEY  (subject_id, rater_id)
) ENGINE=InnoDB ;

create table centerlinestability 
(
  centerlinestability_id int not null,
  centerline_id int not null references centerline(centerline_id),
  maxstartpoints int not null,
  perfectstability float not null, 
  fixedends enum("yes", "no") not null, 
  primary key (centerlinestability_id)
)ENGINE=InnoDB;

create table centerlinestabilitybin 
(
  centerlinestability_id int not null references centerlinestability (centerlinestability_id),
  startpoints int not null,
  voxelcount int not null,
  primary key (centerlinestability_id, startpoints)
) ENGINE=InnoDB;

create table algorithm 
(
  algorithm_id int not null ,
  algorithm varchar(255) not null unique,
  primary key (algorithm_id)
) ENGINE=InnoDB;


create table lcagenepisubject 
(
  subject_id int not null references subject (subject_id),
  gid int not null unique,
  aneurysmcase enum("yes", "no") not null, 
  firstdegree enum("yes", "no") not null, 
  spouse enum("yes", "no") not null, 
  primary key (subject_id, gid)
) ENGINE=InnoDB;

create table tortuositycoordinate
(
    tortuosity_id int not null references centerlinetortuosity(tortuosity_id),
    x int not null,
    y int not null,
    z int not null    
) ENGINE=InnoDB;


create table arteryset
(
	arteryset_id int not null, 
	setname varchar(255) not null unique, 
	primary key(arteryset_id)
) ENGINE=InnoDB;

create table setartery
(
	arteryset_id int not null references arteryset(arteryset_id),
	artery_id int not null references artery(artery_id),
	primary key(arteryset_id, artery_id)
) ENGINE=InnoDB;

create table ratescale
(
	ratescale_id int not null,
	low int not null,
	high int not null,
	primary key (ratescale_id)
) ENGINE=InnoDB;

create table rateexperiment
(
	rateexperiment_id int not null auto_increment,
	ratescale_id int not null references ratescale(ratescale_id),
	rater_id int not null references rater(rater_id),
	expdate date not null,
	primary key (rateexperiment_id)
) ENGINE=InnoDB;

create table rating 
(
	rateexperiment_id int not null references rateexperiment(rateexperiment_id),
	subject_id int not null references subject(subject_id),
	artery_id int not null references artery(artery_id),
	rating int not null,
	primary key(rateexperiment_id, subject_id, artery_id)
) ENGINE=InnoDB;

create table arterydisplaytype
(
	arterydisplaytype_id int not null, 
	arterydisplaytype varchar(255) unique,
	primary key(arterydisplaytype_id)
) ENGINE=InnoDB;

create table arterydisplay
(
	subject_id int not null references subject(subject_id),
	artery_id int not null references artery(artery_id),
	arterydisplaytype_id int not null references arterydisplaytype(arterydisplaytype_id),
	directory varchar(255) not null,
	filename varchar(255) not null
) ENGINE=InnoDB;

create table type
(
	type_id int not null,
	type varchar(255) unique,
	primary key (type_id)
) ENGINE=InnoDB;

create table subjecttype
(
	subject_id int not null references subject(subject_id),
	type_id int not null references type(type_id),
	primary key(subject_id, type_id)
) ENGINE=InnoDB; 

create table subjectarterytype
(
	-- Makes a set of subjects, arteries and types. Training sets of artery images are stored here 
	subject_id int not null references subject(subject_id),
	artery_id int not null references artery(artery_id),
	type_id int not null references type(type_id),
	primary key(subject_id, artery_id, type_id)
) ENGINE=InnoDB; 

create table ethnicity
(
	ethnicity_id int not null,
	ethnicity varchar(255) unique,
	primary key(ethnicity_id)
) ENGINE=InnoDB; 

create table diagnosis 
(
	diagnosis_id int not null,
	diagnosis varchar(255) unique not null,
	ICD9 float null,
	primary key (diagnosis_id)
) ENGINE=InnoDB;

create table subjectdiagnosis
(
	subject_id int not null references subject(subject_id),
	diagnosis_id int not null references diagnosis(diagnosis_id),
	diagnosisdate date null, 
	primary key (subject_id, diagnosis_id)
) ENGINE=InnoDB;

create table subjecttypecount
(
	subject_id int not null references subject(subject_id),
	type_id int not null references type(type_id),
	count int not null, 
	primary key(subject_id, type_id)
) ENGINE=InnoDB; 

