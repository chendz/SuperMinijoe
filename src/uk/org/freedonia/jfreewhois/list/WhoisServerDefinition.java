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

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;


@Root(name="WhoisServerDefinition", strict=false)
public class WhoisServerDefinition {
	@Element(name="name")
	protected String serverName;

	@Element(name="address")
	protected String serverAddress;
	@ElementList(entry="tld", inline=true)
	protected List<String> nametld;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * Gets the value of the address property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getServerAddress() {
        return serverAddress;
    }

    /**
     * Sets the value of the address property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }


    public List<String> getNameTld() {
        if (nametld == null) {
        	nametld = new ArrayList<String>();
        }
        return this.nametld;
    }

}
