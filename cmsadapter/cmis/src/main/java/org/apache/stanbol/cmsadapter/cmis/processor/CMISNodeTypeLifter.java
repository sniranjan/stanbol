package org.apache.stanbol.cmsadapter.cmis.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.RelationshipType;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.stanbol.cmsadapter.cmis.repository.CMISModelMapper;
import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropType;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Resource;

public class CMISNodeTypeLifter {
    private static final Logger logger = LoggerFactory.getLogger(CMISNodeTypeLifter.class);
    // FIXME Make adjustable
    private static final int DESCENDANT_DEPTH = 1000;

    private Session session;
    private OntologyResourceHelper orh;

    public CMISNodeTypeLifter(MappingEngine engine) {
        this.session = (Session) engine.getSession();
        this.orh = engine.getOntologyResourceHelper();
    }

    private List<ObjectType> accumulateTypesOnTree(Tree<ObjectType> tree) {
        List<ObjectType> types = new ArrayList<ObjectType>();
        List<Tree<ObjectType>> children = tree.getChildren();
        types.add(tree.getItem());
        for (Tree<ObjectType> child : children) {
            types.addAll(accumulateTypesOnTree(child));
        }
        return types;
    }

    private List<ObjectType> getAllTypes(String baseType) {
        logger.info("Getting all types");
        List<ObjectType> ret = new ArrayList<ObjectType>();
        ObjectType baseTypeObj = null;
        try {
            baseTypeObj = session.getTypeDefinition(baseType);
        } catch (CmisObjectNotFoundException e) {
            logger.warn("Type not found " + baseType);
            return ret;
        } catch (CmisInvalidArgumentException e) {
            logger.warn("Invalid base type: {}", baseType);
            return ret;
        }
        List<Tree<ObjectType>> types = baseTypeObj.getDescendants(DESCENDANT_DEPTH);
        List<ObjectType> objTypes = new ArrayList<ObjectType>();
        for (Tree<ObjectType> typeTree : types) {
            objTypes.addAll(accumulateTypesOnTree(typeTree));

        }

        objTypes.add(baseTypeObj);
        return objTypes;
    }

    private List<ObjectType> getAllDocumentTypes() throws Exception {
        return getAllTypes(ObjectType.DOCUMENT_BASETYPE_ID);
    }

    private List<ObjectType> getAllFolderTypes() throws Exception {
        return getAllTypes(ObjectType.FOLDER_BASETYPE_ID);
    }

    private List<RelationshipType> getAllRelationshipTypes() throws Exception {
        List<ObjectType> objTypes = getAllTypes(ObjectType.RELATIONSHIP_BASETYPE_ID);
        List<RelationshipType> relTypes = new ArrayList<RelationshipType>(objTypes.size());
        for (ObjectType objType : objTypes) {
            if (objType instanceof RelationshipType) {
                relTypes.add((RelationshipType) objType);
            }
        }
        return relTypes;
    }

    /**
     * Extracts semantics through CMIS interface
     * 
     * @param mappingFileContent
     * @throws Exception
     */
    public void liftNodes() throws RepositoryAccessException {
        try {
            // Create classes for Folder Object Type and its descendants
            createClassesForObjectTypes(getAllFolderTypes());
            // Create classes for Document Object Type and its descendants
            createClassesForObjectTypes(getAllDocumentTypes());
            // create object property definitions for relationship object types
            createObjectPropertyDefForRelationshipTypes(getAllRelationshipTypes());
        } catch (Exception e) {
            throw new RepositoryAccessException("Error at CMIS node type lifting", e);
        }
    }

    // TODO ID ler icin gereken deisiklik 2-
    /**
     * Creates classes for the given Object Types Furthermore, creates subsumption relations using the
     * parentId field
     * 
     * @param objectTypes
     * @throws Exception
     */
    private void createClassesForObjectTypes(List<ObjectType> objectTypes) throws Exception {
        // FIXME What about caching Common Domain model instances
        // for each object type create a class
        for (ObjectType type : objectTypes) {
            OntClass klass = orh.createOntClassByObjectTypeDefinition(CMISModelMapper
                    .getObjectTypeDefinition(type));
            for (PropertyDefinition<?> propDef : type.getPropertyDefinitions().values()) {
                org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition propertyDef = CMISModelMapper
                        .getPropertyDefinition(propDef);
                if (objectPropertyCheck(propertyDef)) {
                    orh.createObjectPropertyByPropertyDefinition(propertyDef,
                        Arrays.asList(new Resource[] {klass}), new ArrayList<Resource>(1));
                } else {
                    orh.createDatatypePropertyByPropertyDefinition(propertyDef,
                        Arrays.asList(new Resource[] {klass}));
                }
            }
        }

        // for each parent id create a superclass relation
        for (ObjectType type : objectTypes) {
            ObjectType parentType = type.getParentType();
            if (parentType != null) {
                OntClass klass = orh.createOntClassByObjectTypeDefinition(CMISModelMapper
                        .getObjectTypeDefinition(type));
                OntClass parentClass = orh.createOntClassByObjectTypeDefinition(CMISModelMapper
                        .getObjectTypeDefinition(parentType));
                klass.addSuperClass(parentClass);
            }
        }
    }

    /**
     * Create ObjectProperty definition for each relationship type
     * 
     * @param relationshipTypes
     */
    public void createObjectPropertyDefForRelationshipTypes(List<RelationshipType> relationshipTypes) {
        for (RelationshipType relType : relationshipTypes) {
            List<Resource> domains = new ArrayList<Resource>();
            List<Resource> ranges = new ArrayList<Resource>();

            List<ObjectType> allowedSourceTypes = relType.getAllowedSourceTypes();
            if (allowedSourceTypes != null) {
                for (ObjectType type : allowedSourceTypes) {
                    OntClass klass = orh.createOntClassByObjectTypeDefinition(CMISModelMapper
                            .getObjectTypeDefinition(type));
                    domains.add(klass);
                }
            }
            List<ObjectType> allowedTargetTypes = relType.getAllowedTargetTypes();
            if (allowedTargetTypes != null) {
                for (ObjectType type : allowedTargetTypes) {
                    OntClass klass = orh.createOntClassByObjectTypeDefinition(CMISModelMapper
                            .getObjectTypeDefinition(type));
                    ranges.add(klass);
                }
            }

            orh.createObjectPropertyByPropertyDefinition(CMISModelMapper.getRelationshipType(relType),
                Arrays.asList(new Resource[] {orh.createUnionClass(domains)}),
                Arrays.asList(new Resource[] {orh.createUnionClass(ranges)}));

        }
        // Create super property relations
        for (RelationshipType relType : relationshipTypes) {
            OntProperty prop = orh.getPropertyByReference(relType.getId());
            ObjectType parentType = relType.getParentType();
            if (parentType != null) {
                // TODO Check if parent type is correctly resolved to an ont class
                OntProperty parentProp = orh.getPropertyByReference(parentType.getId());
                prop.addSuperProperty(parentProp);
            }
        }
    }

    private static Boolean objectPropertyCheck(org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition prop) {
        PropType propType = prop.getPropertyType();
        // TODO consider all object properties
        if (propType == PropType.REFERENCE ) {
            return true;
        }
        return false;
    }
}
