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
package org.apache.stanbol.entityhub.jersey.resource.reconcile;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSite;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteException;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/entityhub/site/{site}/reconcile")
public class ReferencedSiteReconcileResource extends BaseGoogleRefineReconcileResource {
    
    private final Logger log = LoggerFactory.getLogger(ReferencedSiteReconcileResource.class);
    private ReferencedSiteManager _siteManager;
    private final String siteId;
    
    
    
    public ReferencedSiteReconcileResource(@PathParam(value = "site") String siteId) {
       super();
       if (siteId == null || siteId.isEmpty()) {
           log.error("Missing path parameter site={}", siteId);
           throw new WebApplicationException(Response.Status.NOT_FOUND);
       }
       this.siteId = siteId;
    }
    private ReferencedSite getSite() throws WebApplicationException {
        if(_siteManager == null){
            _siteManager = ContextHelper.getServiceFromContext(
                ReferencedSiteManager.class, servletContext);
            if(_siteManager == null){
                throw new IllegalStateException("Unable to lookup ReferencedSite '"
                        +siteId+"' because ReferencedSiteManager service is unavailable!");
            }
        }
        ReferencedSite site = _siteManager.getReferencedSite(siteId);
        if (site == null) {
            String message = String.format("ReferencedSite '%s' not acitve!",siteId);
            log.error(message);
            throw new WebApplicationException(
                Response.status(Status.NOT_FOUND).entity(message).build());
        }
        return site;
    }
    /**
     * @param query
     * @return
     * @throws ReferencedSiteException
     */
    protected QueryResultList<Representation> performQuery(FieldQuery query) throws ReferencedSiteException {
        return getSite().find(query);
    }
    @Override
    protected String getSiteName() {
        return getSite().getId() + "Referenced Site";
    }
    @Override
    protected FieldQuery createFieldQuery() {
        return getSite().getQueryFactory().createFieldQuery();
    }
    
}