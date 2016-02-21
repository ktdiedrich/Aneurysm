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

create view centerlinetortuosityview as select ct.tortuosity_id, arteryname, side, ct.centerline_id, startx, starty, startz, endx, endy, endz, 
	sg.segmentation_id, i.image_id, i.subject_id 
	from centerlinetortuosity ct, artery a, side s, centerline c, segmentation sg, image i where ct.artery_id=a.artery_id and a.side_id=s.side_id 
	and ct.centerline_id=c.centerline_id and c.segmentation_id=sg.segmentation_id and sg.image_id=i.image_id and usable!='no'
	order by i.subject_id, i.image_id, a.artery_id, s.side_id;
	
create view arteryview as select a.artery_id, arteryname, shortname, side from artery a, side s where a.side_id = s.side_id order by arteryname,side; 

create view aneurysmview as select aneurysm_id, shape, arteryname, an.artery_id, side, location 
	from aneurysm an, artery ar, side s where an.artery_id = ar.artery_id and ar.side_id=s.side_id order by arteryname, side, location;
	
create view subjectaneurysmview as select sa.subject_id, sa.aneurysm_id, diameter, sa.note as annote, birthdate, sex, visualicatortuosity, shape, location, arteryname, side 
	from subjectaneurysm sa, subject st, aneurysm am, artery ay, side se 
	where sa.subject_id=st.subject_id and sa.aneurysm_id=am.aneurysm_id and am.artery_id=ay.artery_id and ay.side_id=se.side_id 
	order by subject_id, arteryname, side;
	
create view measurearteryview as select ma.artery_id, a.arteryname, s.side from measureartery ma, artery a, side s where ma.artery_id = a.artery_id and a.side_id = s.side_id;

create view variablearteryview as select va.artery_id, a.arteryname, s.side from variableartery va, artery a, side s where va.artery_id = a.artery_id and a.side_id = s.side_id;

create view selectarteryview as select t.artery_id, a.arteryname, s.side from selectartery t, artery a, side s where t.artery_id = a.artery_id and a.side_id = s.side_id;

create view arterysetview as select ars.arteryset_id, ars.setname, a.artery_id, a.arteryname, s.side from arteryset ars, setartery sa, artery a, side s where ars.arteryset_id=sa.arteryset_id and sa.artery_id=a.artery_id and a.side_id=s.side_id order by arteryset_id, arteryname, side; 

create view measurearteryview as select ars.arteryset_id, ars.setname, a.artery_id, a.arteryname, s.side from arteryset ars, setartery sa, artery a, side s where ars.arteryset_id=sa.arteryset_id and sa.artery_id=a.artery_id and a.side_id=s.side_id and ars.arteryset_id=1 order by arteryset_id, arteryname, side; 

create view variablearteryview as select ars.arteryset_id, ars.setname, a.artery_id, a.arteryname, s.side from arteryset ars, setartery sa, artery a, side s where ars.arteryset_id=sa.arteryset_id and sa.artery_id=a.artery_id and a.side_id=s.side_id and ars.arteryset_id=2 order by arteryset_id, arteryname, side; 

create view selectarteryview as select ars.arteryset_id, ars.setname, a.artery_id, a.arteryname, s.side from arteryset ars, setartery sa, artery a, side s where ars.arteryset_id=sa.arteryset_id and sa.artery_id=a.artery_id and a.side_id=s.side_id and ars.arteryset_id=3 order by arteryset_id, arteryname, side; 

create view subjectarteryview as select b.subject_id, b.artery_id,a.arteryname, s.side, b.arteryshape_id, p.arteryshape from subjectartery b, artery a, arteryshape p, side s where b.artery_id=a.artery_id and b.arteryshape_id=p.arteryshape_id and a.side_id=s.side_id order by subject_id, artery_id;

create view visualicameasurementview as select subject_id, score, rater, v.rater_id from visualicameasurement v, rater r where v.rater_id=r.rater_id order by subject_id, rater_id;

create view subjectarterytypeview as select u.subject_id, u.artery_id, u.type_id, a.arteryname, d.side, t.type, u.value  from subjectarterytype u, subject s, artery a, type t, side d where u.subject_id=s.subject_id and u.artery_id=a.artery_id and u.type_id=t.type_id and a.side_id=d.side_id order by u.type_id, u.artery_id, d.side_id, u.subject_id;

create view rateexperimentview as select e.rateexperiment_id, e.ratescale_id, c.low, c.high, e.rater_id, r.rater, e.expdate from rateexperiment e, ratescale c, rater r where e.ratescale_id=c.ratescale_id and e.rater_id=r.rater_id order by rater, expdate, high;

create view subjecttypeview as select st.subject_id, st.type_id, birthdate, sex, type,  handed from subjecttype st, subject s, type t where st.subject_id=s.subject_id and st.type_id=t.type_id ;

create view subjectview as select subject_id, birthdate, sex, visualicatortuosity, handed, ethnicity, note from subject s, ethnicity e where s.ethnicity_id=e.ethnicity_id;

create view subjectdiagnosisview as select sd.subject_id, sd.diagnosis_id, diagnosis, ICD9, diagnosisdate from subject s, diagnosis d, subjectdiagnosis sd where  s.subject_id=sd.subject_id and d.diagnosis_id=sd.diagnosis_id order by subject_id, diagnosis;

create view subjecttypecountview as select st.subject_id, st.type_id, birthdate, sex, type,  count, handed from subjecttypecount st, subject s, type t where st.subject_id=s.subject_id and st.type_id=t.type_id ;
