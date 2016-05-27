/*===========================================================================+
 |  HISTORY                                                                  
 |                                                                           
 |   DATE       DEVELOPER            WR            DESCRIPTION                               
 |  ====        =========         ==========       =========== 
 |  18-July-14  Bhanu Prakash      PROPEL          Initial creation - For PROPEL FD QOT.CTQ.FD.I.038
 
 ===========================================================================+*/

global class Oppty_Search_IntegrationOperation {
    /*
    * Opportunity search data from AIC
    */
    global class Oppty_SearchData{
        webservice String opptyName;
        webservice String opptyNo;
        webservice String soldToPartyNo;//Partner__r.party_number__c
        webservice String endCustPartyNo;//Account.Party_number__c
        webservice String tier2PartyNo;//Tier_2_Partner__r.Party_number__c
        webservice String distributionChannel;//Sales_Channel__c
        webservice String primarySalesRep;
    }

    /*
    * Opportunity Search result class
    */
    global class Oppty_SearchResponse{
        webservice Boolean isSuccess {set;get;}
        webservice String errorMessage {set;get;}
        webservice List<OpptyWrapper> lstOppWrapResults {set;get;}
    }
    /*
    8 Opportunity wrapper class to send AIC
    */
    global class OpptyWrapper {
        webservice String name;
        webservice String opptyNumber;
        webservice String stageName;
        webservice Double amount; 
        webservice String currencyIsoCode;
        webservice String distributionChannel;
        webservice Date   closeDate;
        webservice String accountName;
        webservice String tier2Partner;  
    }
  
    /*
    * Main web-service method
    */
    webservice static Oppty_SearchResponse performOpptySearch(Oppty_SearchData opptySearchData){
    List<OpptyWrapper> lstOpptyWraper = new List<OpptyWrapper>();
        Oppty_SearchHelper_IntegrationOperation searchHelper = new Oppty_SearchHelper_IntegrationOperation();
        Oppty_SearchResponse opptySearchResponse = new Oppty_SearchResponse();
        try{// Perform search operation
            opptySearchResponse= searchHelper.performOpptyHelperSearch(opptySearchData);
        }catch(Exception e){//On any error set operation is unsuccessful and send error message back
            opptySearchResponse.isSuccess= false;
            opptySearchResponse.errorMessage = Label.PROPEL_Exception + ' : ' + e.getMessage();
        }
    return opptySearchResponse;
    }
}