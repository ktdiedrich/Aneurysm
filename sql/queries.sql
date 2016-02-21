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

-- Querying aneurysm database 
-- author Karl.Diedrich@utah.edu
-- find images with only one ICA artery measured for tortuosity. 
select distinct i.subject_id, artery_id, i.image_id from image i, segmentation s, centerline c, centerlinetortuosity t where i.image_id=s.image_id and s.segmentation_id=c.segmentation_id and c.centerline_id=t.centerline_id and (artery_id =1 or artery_id=2) order by subject_id;

-- one line DFM tortuosity data 
select i.subject_id, i.image_id, ct.artery_id, ct.tortuosity_id, length, distance, dfm from dfm d, centerlinetortuosity ct, centerline c, segmentation g, image i where d.tortuosity_id=ct.tortuosity_id and ct.centerline_id=c.centerline_id and c.segmentation_id=g.segmentation_id and g.image_id =i.image_id and  direction='forward' and ct.artery_id = 5 and i.image_id=480 order by length; 

-- find phantoms
select i.filename, i.directory,  i.image_id, g.segmentation_id from segmentation g, image i where g.image_id=i.image_id and i.note like '%phantom%';