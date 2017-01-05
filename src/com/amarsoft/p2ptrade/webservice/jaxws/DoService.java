
package com.amarsoft.p2ptrade.webservice.jaxws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "doService", namespace = "http://ws.service.amarsoft.com/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "doService", namespace = "http://ws.service.amarsoft.com/", propOrder = {
    "request"
})
public class DoService {

    @XmlElement(name = "request", namespace = "")
    private String request;
    
    /**
     * 
     * @return
     *     returns String
     */
    public String getRequest() {
        return this.request;
    }

    /**
     * 
     * @param request
     *     the value for the method property
     */
    public void setRequest(String request) {
        this.request = request;
    }

}
