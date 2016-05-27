/*
 HISTORY                                                                  
 |                                                                           
 |   DATE            DEVELOPER        WR/Req         DESCRIPTION                               
 |   ====            =========        ======         ===========
 |  3-JAN-2016       BhanuPrakash    VirtuStream    Initial Creatoin
*/

public class GenericOperationsHelper {

    private static final Map<String, Schema.SObjectType> globalDescribe = Schema.getGlobalDescribe();
    public static Boolean runOnce = true;
    public static Map<Id,SObject> newSobjMap1 = null;
    public static String parentObjAPIName = null;
    public static String childObjAPIName = null;
    public static String forenKeyAPIName = null;
    public static String parentFieldToUpdateAPIName = null;
    public static Boolean isParentToParent;
    
    private static final String SELECT_ID = 'SELECT Id';
    private static final String COMMA = ',';
    
    /*
    *   Parameter 1 : parentObjType 
    *   Parameter 2 : recordTypeID  
    */
    public static String getRecordTypeNameById(String parentObjType, Id recTypeId){
        String rtName = null;
        try{
            rtName = globalDescribe.get(parentObjType).getDescribe().getRecordTypeInfosById().get(recTypeId).getName();
        }catch(Exception e){
            System.debug('**** Exception on getRecordTypeNameById ' + e.getMessage());
        }
        return rtName;
    }
    
    /*
    *   Parameter 1 : SObject 
    *   Parameter 2 : Set<Id>   
    *   Parameter 3 : String
    */
    public static void populateRecordsWithRecordTyepeId(List<SObject> sObjectList, String populateFieldName){
        Map<Id, String> objIdRtNameMap = new Map<Id,String>();
        Set<Id> rtIdSet = new Set< Id>();
        for(SObject sobjLoop : sObjectList){
            rtIdSet.add((ID)sobjLoop.get('RecordTypeId'));
        }
        Map<Id, RecordType> rtIdRtMap = new Map<Id, RecordType>([SELECT ID, Name FROM RecordType WHERE ID IN : rtIdSet]);
        try{
            for(SObject sobj1 : sObjectList){
                if( rtIdRtMap.keySet().contains((ID)sobj1.get('RecordTypeId'))) {
                    sobj1.put(populateFieldName, rtIdRtMap.get((ID)sobj1.get('RecordTypeId')).get('Name'));
                }
            }
        }catch(Exception e){
            System.debug('**** Exception on populateRecordsWithRecordTyepeId ' + e.getMessage());
        }
    }
    
    /*
    *   Parameter 1 : parentObjType 
    *   Parameter 2 : recTypeName   
    */
    public static String getRecordTypeIdByName(String parentObjType, String recTypeName){
        Id rtId = null;
        try{
          rtId = globalDescribe.get(parentObjType).getDescribe().getRecordTypeInfosByName().get(recTypeName).getRecordTypeId();
        } catch(Exception e){
            System.debug('**** Exception on getRecordTypeNameByName ' + e.getMessage());
        }
        return rtId;
    }
    
    public static List<SObject> getObjectsFieldValueById(String objectName, String selectField, Id objId){
        List<SObject> tempChaildSobjsList = null;
        String dynaQuery = SELECT_ID;
        dynaQuery += COMMA + selectField;
        
        dynaQuery += ' FROM ' + objectName +  ' WHERE Id=:objId';
        try{
            tempChaildSobjsList = Database.query(dynaQuery);
        } catch(QueryException e){
            System.debug('######### Exception while querying sObject fields : ' +  e.getMessage());
        }
        return tempChaildSobjsList;
    }
    
    public static List<SObject> getObjectsFieldValuesById(String objectName, Set<String> selectFields, Set<Id> objIds){
        List<SObject> tempChaildSobjsList = null;
        String dynaQuery = SELECT_ID;
        if(selectFields != null && !selectFields.isEmpty()){
            for(String fName : selectFields){
                dynaQuery += COMMA + fName;
            }
        }
        dynaQuery += ' FROM ' + objectName +  ' WHERE Id IN :objIds';
        try{
            tempChaildSobjsList = Database.query(dynaQuery);
        } catch(QueryException e){
            System.debug('######### Exception while querying sObject fields : ' +  e.getMessage());
        }
        return tempChaildSobjsList;
    }
    
    
    public static void updateRollUpField(){
        if(newSobjMap1 != null && !String.isBlank(parentObjAPIName) && !String.isBlank(childObjAPIName) && !String.isBlank(forenKeyAPIName) && !String.isBlank(parentFieldToUpdateAPIName)){
                updateChildCountToParentOrPTP(newSobjMap1, parentObjAPIName, childObjAPIName, forenKeyAPIName, parentFieldToUpdateAPIName, isParentToParent);
        }
    }
    
    /*
    *   newSobjMap      : Trigger newMap of SObject type
    *   childObjName    : Chaild object name to query
    *   foregnKey       : Foren Key field name between Parent and chaild objects
    *   parentFieldToUpdate : Field name to update in parent with count of chaild records
    *   This is a generic method to update parent roll-up field with count of chaild records - TRIGGERS FOR CHAILD RECORDS
    */
    public static void updateChildCountToParentOrPTP(Map<Id,SObject> newSobjMap,String parentObjType, String childObjName, String foregnKey,String parentFieldToUpdate,Boolean isParentToParent1) {
        if(runOnce){
            runOnce = false;
            List<SObject> sobjToUpdateList = new List<SObject>();
            Map<Id,Integer> childIdnCount = new Map<Id,Integer>();
            Set<Id> parentIdSet = new Set<Id>();
            List<SObject> childList = null;
            
            if(!isParentToParent1){
                //Get Set of foregnKey values to query on parent object
                for(SObject ocr :  newSobjMap.values()) {
                    Id key = String.ValueOf(ocr.get(foregnKey));
                    parentIdSet.add(key);
                }
            } else {
                parentIdSet = newSobjMap.keySet();
            }
            childList = getChaildsByParentIds(parentIdSet, childObjName,foregnKey, null);
            if( childList != null){
                for(SObject ocr :  childList) {
                    Id key = String.ValueOf(ocr.get(foregnKey));
                    if(childIdnCount.get(key) == null){
                        childIdnCount.put(key,1);
                    }else {
                        childIdnCount.put(key,(childIdnCount.get(key) + 1));
                    }
                }
            }
            
            Set<Id> ids = childIdnCount.keySet();
            String query = SELECT_ID + COMMA + parentFieldToUpdate + ' FROM '+ parentObjType + ' WHERE Id IN : ids';
            List<SObject> sObjList = (List<SObject>) Database.query(query);
            
            for(SObject sobj :  sObjList){
                Id key = String.ValueOf(sobj.get('Id'));
                if(childIdnCount.get(key) != null){
                    Schema.SObjectField field = globalDescribe.get(parentObjType).getDescribe().fields.getMap().get(parentFieldToUpdate);
                    sobj.put(field, childIdnCount.get(key));
                    sobjToUpdateList.add(sobj);
                }
            }
            
            if (sobjToUpdateList.size() > 0) {
                update sobjToUpdateList;
            }
        }
    }
    
    /*
    *   newSobjMap      : Trigger newMap of SObject type
    *   childObjName    : Chaild object name to query
    *   foregnKey       : Foren Key field name between Parent and chaild objects
    *   This is a generic method to update parent roll-up field with count of chaild records - TRIGGERS FOR CHAILD RECORDS
    */
    public static void populateChildCount(Map<Id,SObject> newSobjMap,String parentObjType, String childObjName, String foregnKey, String parentFieldName) {
            Map<Id,Integer> parentIdnChildCount = new Map<Id,Integer>();
        if(runOnce){
            runOnce = false;
            List<SObject> sobjToUpdateList = new List<SObject>();
            Map<Id,Integer> childIdnCount = new Map<Id,Integer>();
            Set<Id> parentIdSet = new Set<Id>();
            List<SObject> childList = null;
            
            childList = getChaildsByParentIds(newSobjMap.keySet(), childObjName,foregnKey, null);
            if( childList != null){
                for(SObject ocr :  childList) {
                    //Parant Id n Child count map creation
                    if(parentIdnChildCount.get((Id)ocr.get(foregnKey)) == null){
                        parentIdnChildCount.put((Id)ocr.get(foregnKey),1);
                    }else {
                        parentIdnChildCount.put((Id)ocr.get(foregnKey),parentIdnChildCount.get((ID)ocr.get(foregnKey)) + 1);
                    }
                }
            }
            for(ID pID : newSobjMap.keySet()){
                if(parentIdnChildCount != null && parentIdnChildCount.get(pID) != null ){
                    newSobjMap.get(pID).put(parentFieldName, parentIdnChildCount.get(pID));
                } else {
                    newSobjMap.get(pID).put(parentFieldName, 0);
                }
            }
        }

        //return parentIdnChildCount;
    }
    /*
    *   parentIds   : Set of Parent Id's to query chaild records
    *   childObjName: Chaild object API name
    *   foregnKey   : Relation key between parent and chaild
    *   selectFields: Set of field names to retrive from child record
    *   Method to dynamically query on chaild object and return list of them
    */
    public static List<SObject> getChaildsByParentIds(Set<Id> parentIds, String childObjName, String foregnKey, Set<String> selectFields) {
        List<SObject> tempChaildSobjsList = null;
        String dynaQuery = SELECT_ID;
        if(selectFields != null && !selectFields.isEmpty()){
            for(String fName : selectFields){
                dynaQuery += COMMA + fName;
            }
        }
        dynaQuery += COMMA + foregnKey + ' FROM ' + childObjName + ' WHERE ' + foregnKey + ' IN :parentIds';
        try{
            tempChaildSobjsList = Database.query(dynaQuery);
        }catch(QueryException e){
            System.debug('######### Exception while querying Chailds : ' +  e.getMessage());
        }
        return tempChaildSobjsList;
    }
    
    /*
    *   
    */
    public static void addErrByParentFieldValue(Map<Id,SObject> childMap,String parentName, String forenKey,  String parentFieldName, String parentFieldValues, String errorMsg ){
        
        Map<Id,Id> childIDNParentIdMap = new Map<Id,Id>();
        Set<Id> parentIdSet = new Set<Id>();
        //Split field vlaues to add in query as AND conditions
        String[] fieldValues = parentFieldValues.split(',');
        
        //COLLECT SET OF PARENT ID's
        for(SObject sobj : childMap.values()){
            //Prepare PL id and Opportunity Id map
            childIDNParentIdMap.put((Id)sobj.get('Id'), (ID)sobj.get(forenKey));
            parentIdSet.add( (ID)sobj.get(forenKey));
        }
        
        //
        if(parentIdSet.size() != 0){
            //GET PARENT IDS BASED ON FIELD VALUES PASSED INTO METHOD
            Set<ID> parentFilteredIdSet = getParentIdsWithFieldValue(parentIdSet, parentName, parentFieldName, fieldValues);
            for(ID cId : childMap.keySet()){
                //if(parentFilteredIdSet.contains(childMap.get(cId).get(forenKey)) != null){
                if(parentFilteredIdSet.contains((ID)childMap.get(cId).get(forenKey)) ){
                    childMap.get(cId).addError(errorMsg);
                    ApexPages.Message myMsg = new ApexPages.Message(ApexPages.Severity.ERROR,'Error: Opportunity is in Closed Stage');
                }
            }
        }
    }
    
    public static Set<Id> getParentIdsWithFieldValue(Set<Id> parentIds, String parentObjType, String parentField, String[] values) {
        Map<Id,SObject> childMap = null;
        
        String dynaQuery = SELECT_ID;
         dynaQuery = 'Select Id, ' + parentField + ' From ' +  parentObjType + ' WHERE ID IN : parentIds  ';
        if(values != null && values.size() > 0 ){
            Integer loopCount = 0;
            for(Integer i=0;i <  values.size(); i++){
                if(loopCount == 0){
                    dynaQuery += ' AND (' + parentField + ' =\'' + values[i] + '\'';
                }else {
                    dynaQuery += ' OR ' + parentField + ' =\'' + values[i] + '\'';
                }
                ++loopCount;
            }
            dynaQuery += ')';
        }
        
        List<SObject> sobj = Database.query(dynaQuery);
        
        childMap = new Map<Id,SObject>(sobj);
        
        if(childMap.keySet().size() != null){
            return childMap.keySet();
        }
        return null;
    }
    public class VirtuStreamException extends Exception {}
}