/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.enhancer.servicesapi;

import java.io.InputStream;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;

/**
 * A unit of content that Stanbol Enhancer can enhance.
 * <p>
 * Gives access to the binary content that
 * was registered, and the Graph that represents its metadata
 * (provided by client and/or generated).
 */
public interface ContentItem {

    /** The Uri of this ContentItem (either supplied by client or generated by Stanbol Enhancer) */
    UriRef getUri();

    /**
     * The binary content stream. Shortcut for
     * <code>{@link #getBlob()}{@link Blob#getStream() .getStream()}</code>
     * @return the InputStream
     */
    InputStream getStream();

    /**
     * The MimeType. Shortcut for
     * <code>{@link #getBlob()}{@link Blob#getMimeType() .getMimeType()}</code>.
     * @return the MimeType as string
     */
    String getMimeType();

    /** Optional metadata */
    MGraph getMetadata();
    
    /**
     * The main content of this content item
     * 
     * @return the blob at index 0
     */
    Blob getBlob();
    
    /**
     * A content item may consists of multiple parts, while the part with index 0 should always be a blob,
     * higher position may be used by Enhancer to story arbitrary objects, such objects can be used for 
     * accessing the precomputations of EnhancementEngines previous in the chain.
     * 
     */
    <T> T getPart(int index, Class<T> clazz) throws NoSuchPartException;
    
    /**
     * Each part of the content item has a URI. EnhancementEngines typically access parts by their Uri as the
     * position may vary depending on the chain.
     */
    <T> T getPart(UriRef uri, Class<T> clazz) throws NoSuchPartException;
    
    /**
     * Get the uri of the part at the specified index
     */
    UriRef getPartUri(int index) throws NoSuchPartException;

    /**
     * Add a new part to this ContentItem
     * 
     * @param uriRef the URI of the part
     * @param object the part
     * @return the part replaced by the parsed object or <code>null</code> if
     * no part with the parsed URI was present
     */
    Object addPart(UriRef uriRef, Object object);
}
