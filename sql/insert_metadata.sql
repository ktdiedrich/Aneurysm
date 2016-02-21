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

-- insert metadata, permanent data used by other tables in the database
-- this needs to start with an empty database 
-- @author Karl.Diedrich@utah.edu

insert into side(side_id, side) values (0, 'single');
insert into side(side_id, side) values(1, 'left');
insert into side(side_id, side) values(2, 'right');
insert into brand(brand_id, brand) values(1, 'GE');
insert into brand(brand_id, brand) values(2, 'Siemens');
insert into mri(mri_id, tesla, brand_id) values(1, 1.5, 1);
insert into mri(mri_id, tesla, brand_id) values(2, 3.0, 2);
insert into artery(artery_id, arteryname, side_id) values(0,   'Unspecified', 0);
insert into artery(artery_id, arteryname, side_id) values(1,   'Internal Carotid Artery',  1);
insert into artery(artery_id, arteryname, side_id) values(2,   'Internal Carotid Artery',   2);
insert into artery(artery_id, arteryname, side_id) values(3,   'Anterior Cerebral Artery', 1);
insert into artery(artery_id, arteryname, side_id) values(4,   'Anterior Cerebral Artery', 2);
insert into artery(artery_id, arteryname, side_id) values(5,   'Vertebral Artery',                 1);
insert into artery(artery_id, arteryname, side_id) values(6,   'Vertebral Artery',                 2);
insert into artery(artery_id, arteryname, side_id) values(7,   'Basilar Artery',                     0);
insert into artery(artery_id, arteryname, side_id) values(8,   'L to R Anterior Cerebral', 0);
insert into artery(artery_id, arteryname, side_id) values(9,   'L to R Anterior Comm',         0);
insert into artery(artery_id, arteryname, side_id) values(10, 'Posterior Communicating',   1);
insert into artery(artery_id, arteryname, side_id) values(11, 'Posterior Communicating',   2);

insert into measureartery(artery_id) values(1);
insert into measureartery(artery_id) values(2);
insert into measureartery(artery_id) values(5);
insert into measureartery(artery_id) values(6);
insert into measureartery(artery_id) values(7);
insert into measureartery(artery_id) values(8);

insert into variableartery(artery_id) values(3);
insert into variableartery(artery_id) values(4);
insert into variableartery(artery_id) values(5);
insert into variableartery(artery_id) values(6);
insert into variableartery(artery_id) values(9);
insert into variableartery(artery_id) values(10);
insert into variableartery(artery_id) values(11);


insert into arteryshape(arteryshape_id, arteryshape) values(1, 'present');
insert into arteryshape(arteryshape_id, arteryshape) values(2, 'absent');
insert into arteryshape(arteryshape_id, arteryshape) values(3, 'short');
insert into arteryshape(arteryshape_id, arteryshape) values(4, 'narrow');
insert into arteryshape(arteryshape_id, arteryshape) values(5, 'thick');
insert into arteryshape(arteryshape_id, arteryshape) values(6, 'short narrow');
insert into arteryshape(arteryshape_id, arteryshape) values(7, 'short thick');



insert into aneurysm(aneurysm_id, shape, artery_id, location) values(0, 'no aneurysm',  0, 'no aneurysm');
insert into aneurysm(aneurysm_id, shape, artery_id, location) values(1, 'Unspecified', 1, 'Unspecified');
insert into aneurysm(aneurysm_id, shape, artery_id, location) values(2, 'Unspecified', 2, 'Unspecified');
insert into aneurysm(aneurysm_id, shape, artery_id, location) values(3, 'Unspecified', 3, 'Unspecified');
insert into aneurysm(aneurysm_id, shape, artery_id, location) values(4, 'Unspecified', 4, 'Unspecified');
insert into aneurysm(aneurysm_id, shape, artery_id, location) values(5, 'Unspecified', 5, 'Unspecified');
insert into aneurysm(aneurysm_id, shape, artery_id, location) values(6, 'Unspecified', 6, 'Unspecified');
insert into aneurysm(aneurysm_id, shape, artery_id, location) values(7, 'Unspecified', 7, 'Unspecified');



-- insert into image(filename, directory, mri_id, xres, yres, zres) values('dummy','dummy/dir', 1, .25, .25, .5);
