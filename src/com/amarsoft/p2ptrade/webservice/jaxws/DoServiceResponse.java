
package com.amarsoft.p2ptrade.webservice.jaxws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "doServiceResponse", namespace = "http://ws.service.amarsoft.com/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "doServiceResponse", namespace = "http://ws.service.amarsoft.com/")
public class DoServiceResponse {

    @XmlElement(name = "doService", namespace = "")
    private String doService;

    /**
     * 
     * @return
     *     returns String
     */
    public String getDoService() {
        return this.doService;
    }

    /**
     * 
     * @param mobileChangeNotify
     *     the value for the runService property
     */
    public void setDoService(String doService) {
        this.doService = doService;
    }

}
