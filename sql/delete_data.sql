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

-- delete inserted data from running programs from the tables 
-- but leave metadata tables data
-- @author Karl.Diedrich@utah.edu 

delete from dfm;
delete from centerlinetortuosity;
delete from image;
delete from subject;
delete from centerline;
delete from segmentation;
delete from centerlinestabilitybin;
delete from centerlinestability;