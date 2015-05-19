/*******************************************************************************
 * Copyright (c) 2013 Joe Beeton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Joe Beeton - initial API and implementation
 ******************************************************************************/
package uk.org.freedonia.jfreewhois.list;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;


@Root(name="WhoisServerList", strict=false)
public class WhoisServerList {

	@ElementList(entry="WhoisServerDefinition", inline=true)
    protected List<WhoisServerDefinition> whoisServerDefinitions;

   
   public List<WhoisServerDefinition> getWhoisServerDefinitions() {
	   if (whoisServerDefinitions == null) {
        	whoisServerDefinitions = new ArrayList<WhoisServerDefinition>();
        }
        return this.whoisServerDefinitions;
    }

}
