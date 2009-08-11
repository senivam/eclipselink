/*******************************************************************************
* Copyright (c) 1998, 2009 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
* which accompanies this distribution.
* The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
* and the Eclipse Distribution License is available at
* http://www.eclipse.org/org/documents/edl-v10.php.
*
* Contributors:
* bdoughan - August 5/2009 - 2.0 - Initial implementation
******************************************************************************/
package org.eclipse.persistence.internal.jaxb;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.oxm.XMLMarshaller;
import org.eclipse.persistence.oxm.XMLRoot;
import org.eclipse.persistence.oxm.XMLUnmarshaller;
import org.eclipse.persistence.oxm.mappings.converters.XMLConverter;
import org.eclipse.persistence.sessions.Session;

/**
 * Convert between instances of XMLRoot and JAXBElement 
 */
public class JAXBElementRootConverter implements XMLConverter {

    private Class declaredType;
    private XMLConverter nestedConverter;

    public JAXBElementRootConverter(Class declaredType) {
        this.declaredType = declaredType;
    }

    public Converter getNestedConverter() {
        return nestedConverter;
    }

    public void setNestedConverter(XMLConverter nestedConverter) {
        this.nestedConverter = nestedConverter;
    }

    public void initialize(DatabaseMapping mapping, Session session) {
        if(null != nestedConverter) {
            nestedConverter.initialize(mapping, session);
        }
    }

    public Object convertDataValueToObjectValue(Object dataValue, Session session) {
        return this.convertDataValueToObjectValue(dataValue, session, null);
    }

    public Object convertDataValueToObjectValue(Object dataValue, Session session, XMLUnmarshaller unmarshaller) {
        if(null != nestedConverter) {
            dataValue = nestedConverter.convertDataValueToObjectValue(dataValue, session, unmarshaller);
        }
        if(dataValue instanceof JAXBElement) {
            return dataValue;
        } else if(dataValue instanceof XMLRoot) {
            XMLRoot root = (XMLRoot)dataValue;
            QName name = new QName(root.getNamespaceURI(), root.getLocalName());
            dataValue = root.getObject();
            if(null == dataValue) {
                return createJAXBElement(name, Object.class, dataValue);
            }else{
                return createJAXBElement(name, declaredType, dataValue);
            }
        }
        return dataValue;
    }

    public Object convertObjectValueToDataValue(Object objectValue, Session session) {
        return this.convertObjectValueToDataValue(objectValue, session, null);
    }

    public Object convertObjectValueToDataValue(Object objectValue, Session session, XMLMarshaller marshaller) {
        if(null != nestedConverter) {
            objectValue = nestedConverter.convertObjectValueToDataValue(objectValue, session, marshaller);
        }
        if(objectValue instanceof JAXBElement && !(objectValue instanceof WrappedValue)) {
            JAXBElement element = (JAXBElement) objectValue;
            XMLRoot root = new XMLRoot();
            root.setLocalName(element.getName().getLocalPart());
            root.setNamespaceURI(element.getName().getNamespaceURI());
            root.setObject(element.getValue());
            return root;
        }
        return objectValue;
    }

    public boolean isMutable() {
        return false;
    }

    private JAXBElement createJAXBElement(QName qname, Class theClass, Object value){
        if(value != null && value instanceof JAXBElement){
            return (JAXBElement)value;
        }
        if(ClassConstants.XML_GREGORIAN_CALENDAR.isAssignableFrom(theClass)){
            theClass = ClassConstants.XML_GREGORIAN_CALENDAR;
        }else if(ClassConstants.DURATION.isAssignableFrom(theClass)){
            theClass = ClassConstants.DURATION;
        }
        return new JAXBElement(qname, theClass, value);
    }

}