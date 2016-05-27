/*===========================================================================+
 |  HISTORY                                                                  
 |                                                                           
 |  DATE       DEVELOPER            WR            DESCRIPTION                               
 |  ====       =========        ==========        =========== 
 |  28-July-14  Bhanu Prakash     PROPEL          Initial creation - For PROPEL FD QOT.CTQ.FD.I.038
 |  30-Sept-14  Bhanu Prakash     PROPEL          Revised query preparation logic and added OpptyWrapper implementation
 |  22-Dec-14   Bhanu Prakash     PROPEL          Fixed Order by name issue
 |  27-Mar-15   Bhanu Prakash     PROPEL          Fixed Defect ID : 19089 by replacing 'party_number__c' with 'UCID__c' field
 ===========================================================================+*/

public class Oppty_SearchHelper_IntegrationOperation {
    public static Oppty_Search_IntegrationOperation.Oppty_SearchData searchData = null;
    public static Oppty_Search_IntegrationOperation.Oppty_SearchResponse staticSearchResponse = null;
    public static String exceptionDescription = null;
    static Boolean isSOSL = false;
    /*
    * Method to perform all search functionalities 
    */
    public Oppty_Search_IntegrationOperation.Oppty_SearchResponse performOpptyHelperSearch(Oppty_Search_IntegrationOperation.Oppty_SearchData sData){
       searchData = sData;
        String query= null;
        staticSearchResponse = new Oppty_Search_IntegrationOperation.Oppty_SearchResponse();
        try{
            query = prepareQuery(searchData);//Prepare & get query based on criteria
            staticSearchResponse = performSearch(query, searchData);//Perform Oppty search and do salesRep access validation then get list of Opptys
        }catch(Exception e){
            if(exceptionDescription!=null){
                staticSearchResponse.errorMessage = exceptionDescription;
            }else {
                staticSearchResponse.errorMessage = Label.PROPEL_Exception +' : ' + e.getMessage();
            }
            staticSearchResponse.isSuccess = false;
        }
        return staticSearchResponse;
    }
    
    /*
    * Method to construct SOQL query based of search criteria.
    */
    private String prepareQuery(Oppty_Search_IntegrationOperation.Oppty_SearchData searcData){
        String query = null;
        String queryWHERE= 'WHERE';
        String fullQuary = null;
        //30505 to make the incoming request parameter for opportunity number as Case Insensitive
       if(searcData.opptyNo!=null && searcData.opptyNo!='') {
          searcData.opptyNo= searcData.opptyNo.toUppercase();
       }
       //Prepare basic query
       query = 'SELECT  Name, Opportunity_Number__c,StageName,Amount,CurrencyIsoCode,Sales_Channel__c,CloseDate,Account_Name1__c,Tier_2_Partner__r.Name FROM Opportunity ';
        
        //if distributionChannel /primarySalesRep null/empty raise an exception as they are mandatory fields.
        if((searcData.distributionChannel ==null || searcData.distributionChannel =='') || (searcData.primarySalesRep  ==null || searcData.primarySalesRep  =='')){
            //staticSearchResponse = new Oppty_Search_IntegrationOperation.Oppty_SearchResponse();
            staticSearchResponse.isSuccess=false;
            if(searcData.distributionChannel ==null || searcData.distributionChannel ==''){//distributionChannel is mandatory
                exceptionDescription = Label.PROPEL_Mandatory_DestributionChannel;
            }
            if(searcData.primarySalesRep  ==null || searcData.primarySalesRep  ==''){//primarySalesRep is mandatory
                exceptionDescription = Label.PROPEL_Mandatory_SalesRep;
            }
            throw new SearchException(Label.PROPE_Mandatory_Field_Missing);
        }else if(searcData.opptyNo==null || searcData.opptyNo==''){//If opptyNo is provided, then soldToPartyNo or endCustPartyNo is no longer mandatory
            staticSearchResponse.isSuccess=false;
            //If soldToPartyNo /endCustPartyNo is null/empty raise an exception as they are mandatory fields
                if((searcData.soldToPartyNo ==null || searcData.soldToPartyNo=='') || (searcData.endCustPartyNo  ==null || searcData.endCustPartyNo  =='')){
                    if(searcData.soldToPartyNo ==null || searcData.soldToPartyNo==''){//soldToPartyNo is mandatory if opptyNo is not provided
                        exceptionDescription = Label.PROPEL_Mandatory_PartyNumber;
                    }
                    if(searcData.endCustPartyNo  ==null || searcData.endCustPartyNo  ==''){//endCustPartyNo is mandatory if opptyNo is not provided
                        exceptionDescription = Label.PROPEL_Mandatory_EndCustomerPartyNumber;
                    }
                    throw new SearchException(Label.PROPE_Mandatory_Field_Missing);
                }
            }
         String searchSOSLQuery = null;
         
        //Create Custom setting to check is distribution channel is 'Indirect'
        Propel_General_Settings__c propelSettings = Propel_General_Settings__c.getInstance();
        Set<String> setValues = new Set<String>(propelSettings.Indirect_Distribution_Channels__c.split(';'));
        
        //Add criteria for Opportunity Number
       if(searcData.opptyNo!=null && searcData.opptyNo!=''){
            if(!queryWHERE.endsWith('WHERE')){
                    queryWHERE += ' AND';
            }
            queryWHERE+=' Opportunity_Number__c=\'' + String.escapeSingleQuotes(searcData.opptyNo) +'\'';
            
            if (setValues==null || setValues.isEmpty()){
                exceptionDescription = Label.PROPEL_define_CS_DistributionChannel;
                throw new SearchException(Label.PROPEL_define_CS_DistributionChannel);
            }else {//Add soldToPartyNo criteria to query
                if(searcData.distributionChannel !=null && setValues.contains(searcData.distributionChannel)){
                    if(searcData.soldToPartyNo!= null && searcData.soldToPartyNo != ''){
                        if(!queryWHERE.endsWith('WHERE')){
                            queryWHERE += ' AND';
                        }
                        queryWHERE +=' (partner__r.UCID__c =\'' + String.escapeSingleQuotes(searcData.soldToPartyNo) + '\'' + ' OR partner__r.Partner_Quoting_Account_Name__r.UCID__c =\'' + String.escapeSingleQuotes(searcData.soldToPartyNo) + '\')';
                    }          
                }
            }
            //Bhanu : Code to fix defect : 20435 - END
        } else {
            //Add Oppty name wild-card search criteria
            if(searcData.opptyName!=null && searcData.opptyName!='') {
                if(!queryWHERE.endsWith('WHERE')){
                    queryWHERE += ' AND';
                }
                isSOSL = true;
                searchSOSLQuery = 'FIND \''+ String.escapeSingleQuotes(searcData.opptyName) +'*\' IN ALL FIELDS RETURNING Opportunity(Name, Opportunity_Number__c,StageName,Amount,CurrencyIsoCode,Sales_Channel__c,CloseDate,Account_Name1__c,Tier_2_Partner__r.Name '; 
            }
            //The logic for end customer should only apply if opportunity number is null/empty string
            //query Account based on endCustPartyNo and fetch 'District_Lookup__c' and 'Account_District__c' - START
            List<Account> endCustomer= null;
            endCustomer = [SELECT District_Lookup__c, Account_District__c FROM Account WHERE UCID__c=:searcData.endCustPartyNo Limit 200];
            
            if(endCustomer ==null || endCustomer.isEmpty()){
                exceptionDescription = Label.PROPEL_Mandatory_EndCustomer;
                throw new SearchException(Label.PROPEL_Mandatory_EndCustomer);
            } else {
                if(endCustomer[0].District_Lookup__c != null && endCustomer[0].Account_District__c != null){
                    if(!queryWHERE.endsWith('WHERE')){
                        queryWHERE += ' AND';
                    }
                    queryWHERE +=' account.District_Lookup__c=\'' + String.escapeSingleQuotes(endCustomer[0].District_Lookup__c) +'\'';
                    queryWHERE +=' AND account.Account_District__c=\'' + String.escapeSingleQuotes(endCustomer[0].Account_District__c) +'\'';
                } else {//Added code as per defect 17005 on Cycle0 testing
                    if(!queryWHERE.endsWith('WHERE')){
                        queryWHERE += ' AND';
                    }
                    queryWHERE +=' account.UCID__c =\'' + String.escapeSingleQuotes(searcData.endCustPartyNo) +'\'';
                }
            }
            //Get CustomSetting to check distributionChannel value(Distributor, Tier 1 Reseller or Service Provider)
            if (setValues==null || setValues.isEmpty()){
                exceptionDescription =Label.PROPEL_define_CS_DistributionChannel;
                throw new SearchException(Label.PROPEL_define_CS_DistributionChannel);
            }
            if(searcData.distributionChannel !=null && setValues.contains(searcData.distributionChannel)){
                if(searcData.soldToPartyNo!= null && searcData.soldToPartyNo != ''){
                    if(!queryWHERE.endsWith('WHERE')){
                        queryWHERE += ' AND';
                    }
                    queryWHERE +=' (partner__r.UCID__c =\'' + String.escapeSingleQuotes(searcData.soldToPartyNo) + '\'' + ' OR partner__r.Partner_Quoting_Account_Name__r.UCID__c =\'' + String.escapeSingleQuotes(searcData.soldToPartyNo) + '\')';//??? PENDING : EXCEPTION ON RELATION
                }
                if(searcData.tier2PartyNo != null && searcData.tier2PartyNo != ''){
                    if(!queryWHERE.endsWith('WHERE')){
                        queryWHERE += ' AND';
                    }
                    queryWHERE +=' Tier_2_Partner__r.UCID__c=\'' + String.escapeSingleQuotes(searcData.tier2PartyNo) + '\'';
                }           
            }
        }//else - end
       
        //Add Oppty Forecast Status filter
        if(!queryWHERE.endsWith('WHERE')) {
            queryWHERE += ' AND';
        }
        queryWHERE += ' (StageName IN(\'Pipeline\',\'Strong Upside\', \'Upside\',\'Won\', \'Commit\'))';
        
        //Add ORDER BY clouse to query
        queryWHERE += ' ORDER BY name';
        
        //add limit to query to 200
        queryWHERE +=' LIMIT 100';
        
        if(isSOSL){
            fullQuary = searchSOSLQuery + queryWHERE + ')';
        } else{
            fullQuary = query + queryWHERE;
        }
        return fullQuary;
    }
    /*
    * Method to query the opportunities and filter according to sales rep access
    */
    private Oppty_Search_IntegrationOperation.Oppty_SearchResponse performSearch(String query, Oppty_Search_IntegrationOperation.Oppty_SearchData searData){
        Oppty_Search_IntegrationOperation.Oppty_SearchResponse searchResponse1 = new Oppty_Search_IntegrationOperation.Oppty_SearchResponse();
        List<Opportunity> lisOppty = null;
        List<Opportunity> lisResultOpptys = null;
        try{
            if(isSOSL){
                List<List<SObject>> objects = search.query(query);
                lisOppty = (List<Opportunity>)objects[0];
            } else {
                lisOppty = Database.query(query);
            }           
        }catch(Exception e){
            exceptionDescription = Label.PROPEL_Err_SearchOppty;
            throw new SearchException(Label.PROPEL_Err_SearchOppty);
        }
        //if no opportunities are found set isSuccess false and return
        if(lisOppty.isEmpty()){
            exceptionDescription = Label.PROPEL_Err_NoMatchingOppts;
            throw new SearchException(Label.PROPEL_Err_NoMatchingOppts);
        } else {//if found some opptys
            //Filter Oppty records according to sales rep access
            lisResultOpptys = filterSalesRepOpptys(lisOppty,searData);
        }
        
        if(lisResultOpptys== null || lisResultOpptys.isEmpty()){
            exceptionDescription = Label.PROPEL_Err_NoOpptyWithRep;
            throw new SearchException(Label.PROPEL_Err_NoOpptyWithRep);
        } 
        List<Oppty_Search_IntegrationOperation.OpptyWrapper> lstOpptyWraper = new List<Oppty_Search_IntegrationOperation.OpptyWrapper>();
        //Create OpptyWrapper object from final result of list of Opptys to send back
        if(lisResultOpptys != null && !lisResultOpptys.isEmpty()){
            Oppty_Search_IntegrationOperation.OpptyWrapper oppWrap = null;
            for(Opportunity oppty : lisResultOpptys){
                oppWrap = new Oppty_Search_IntegrationOperation.OpptyWrapper();
                oppWrap.name = oppty.Name;
                oppWrap.opptyNumber = oppty.Opportunity_Number__c;
                oppWrap.stageName = oppty.StageName;
                oppWrap.amount = oppty.Amount; 
                oppWrap.currencyIsoCode = oppty.CurrencyIsoCode;
                oppWrap.distributionChannel = oppty.Sales_Channel__c;
                oppWrap.closeDate = oppty.CloseDate;
                oppWrap.accountName = oppty.Account_Name1__c;
                oppWrap.tier2Partner = oppty.Tier_2_Partner__r.Name;
                lstOpptyWraper.add(oppWrap);
            }   
        }
        //? FIX : IF INPUT VALUES ARE INVALIED RAISING "Exception in Main class Attempt to de-reference a null object" EXCEPTION
        if (lstOpptyWraper != null && !lstOpptyWraper.isEmpty()){
            searchResponse1.isSuccess=true; 
            searchResponse1.lstOppWrapResults = lstOpptyWraper;
        }
        return searchResponse1;
    }
    /*
    * Method to filter opportunities based on SalesRep access
    */
    private List<Opportunity> filterSalesRepOpptys(List<Opportunity> lstOptyToFilter,Oppty_Search_IntegrationOperation.Oppty_SearchData seaData){
        Map<Id,Opportunity> mapOfOptys = new Map<Id,Opportunity>(lstOptyToFilter);
        
        Set<Id> setUnSortedOpptysIds = new Set<Id>();
        List<Opportunity> lstfilteredOpptys = new List<Opportunity>();
        List<Opportunity> limitedOpptys = new List<Opportunity>();
        try{
            Id salesRepId = seaData.primarySalesRep;
        }catch(Exception e){
            exceptionDescription = Label.PROPEL_Err_InvaliedSalesRepID;
            throw new SearchException(Label.PROPEL_Err_InvaliedSalesRepID);
        }
        Id uid = seaData.primarySalesRep;
        Map<Id,UserRecordAccess> mapURA = new Map<Id,UserRecordAccess>([SELECT RecordId FROM UserRecordAccess WHERE HasReadAccess = true AND UserId =:uid AND RecordId=:mapOfOptys.keyset() Limit 500]);
        Integer count= 0;
        
        for(UserRecordAccess ura : mapURA.values()){
            setUnSortedOpptysIds.add(ura.RecordId);
        }
        
         //Sort the Opportunities on order
        if(mapURA!=null && mapURA.size()>0){
                for(Opportunity opp : lstOptyToFilter){
                    if(setUnSortedOpptysIds.contains(opp.Id)){
                        lstfilteredOpptys.add(opp);
                        count++;
                        if(count==50){
                            break;
                        }
                    }
                }
        }
        //return all list of opportunities
        return lstfilteredOpptys;
    }
    public class SearchException extends Exception {}
}