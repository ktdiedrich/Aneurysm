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

package ktdiedrich.db.aneurysm;

/** 
 * @author ktdiedrich@gmail.com */
public class SubjectArtery
{
	private int _subjectId, _arteryId;
	private String _arteryName, _side;
	public SubjectArtery(int subjectId, int arteryId, String arteryName, String side)
	{
		_subjectId = subjectId;
		_arteryId = arteryId;
		_arteryName = arteryName;
		_side = side;
	}
	public String getArterySideName()
	{
		return _arteryName+" "+_side;
	}
	public String getArteryName() {
		return _arteryName;
	}
	public void setArteryName(String arteryName) {
		_arteryName = arteryName;
	}
	public int getSubjectId() {
		return _subjectId;
	}
	public void setSubjectId(int subjectId) {
		_subjectId = subjectId;
	}
	public int getArteryId() {
		return _arteryId;
	}
	public void setArteryId(int arteryId) {
		_arteryId = arteryId;
	}
	public String toString()
	{
		return "subject ID: "+_subjectId+", artery: "+_arteryId+" "+_arteryName+" "+_side;
	}
	public String getSide() {
		return _side;
	}
	public void setSide(String side) {
		_side = side;
	}
}
