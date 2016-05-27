/*===========================================================================+
 |  HISTORY                                                                  
 |                                                                           
 |  DATE       DEVELOPER        WR       DESCRIPTION                               
 |  ====       =========        ==       =========== 
 |  16-Dec-2014 Bhanuprakash  PROPEL     Created new controller for new hybris Create Quote action  
 |  24-Apr-2015 Bhanuprakash  PROPEL     Fixed defect # 20082 by handling exception on update oppty 
 |  17-Jun-2015 Bhanuprakash  PROPEL     bulkified error messages    
 |  17-Jun-2015 Bhanuprakash  PROPEL      Fix Defect 24296
 |  30-Oct-2015 Bhanuprakash  PROPEL      Fix Defect 32260
  +===========================================================================*/

public with sharing class Oppty_CreateQuoteController{
 //Declaring class variables.
   public Boolean showBackLink{set;get;}
   public String createQuoteEncryptKey;
   public static PROPEL_General_Settings__c gCS;
   public static PROPEL_Hybris_URL_ParamNames__c hybrisUrlParamNames;
   private Blob key;
   private String iv;
   private Blob ivBinary;
   private String Ivb64;
   private Blob dataValue;
   private Blob encrypted;
   private String encryptedb64;
   private String queryString = '?';
   private List<String> lstErrMsgs;
   
   public Opportunity  quriedOppty {get;set;}
   public Opportunity currentOppty{get;set;}
   /*
   *   Class Constructor
   */
   public Oppty_CreateQuoteController(ApexPages.StandardController controller){
        this.currentOppty=(Opportunity)controller.getRecord();
        //Get encrypt base64 format
        gCS = PROPEL_General_Settings__c.getInstance();
        hybrisUrlParamNames = PROPEL_Hybris_URL_ParamNames__c.getInstance();
        createQuoteEncryptKey = gCS.Create_Quote_Encrypt_Key__c;
        iv = generateRandomString(16);
        ivBinary = Blob.valueOf(iv); 
        ivb64 = EncodingUtil.base64encode(ivBinary); // this is the iv value that will be sent to Hybris
   }//Opp_CreateQuote()
   
   // Perform Validation
   /*   Opens a VF page that will validate the following 
        Update and save the opportunity to verify if there are any errors
        Validate that Sales Channel and Sales Force are populated
        Validate that Quote # is not already present
        Check that the user can edit the record (by querying UserRecordAccess for edit rights)
        If there are errors, display the error in a page message and give them a button to navigate back to the opportunity
    */
   /*
   *   Method to validate
   */
   public PageReference validate() {
        try{//Update and save the opportunity to verify if there are any errors
            update currentOppty;
            //throw new CreateQuoteException(System.Label.PROPEL_User_need_Edit_access);
        }catch(Exception e){
            ApexPages.addmessages(e);
             showBackLink=true; return null;
        }//end catch
        try       {
           if(currentOppty.Id!=null) {
              String opportId=currentOppty.id+''; 
              if(opportId.subString(0,3)=='006') {
                   quriedOppty = [select o.Id, o.Name,o.Edit_Access__c ,o.Quote_Operating_Unit__c,o.Sell_Relationship__c,o.Update_Forecast_Amount_from_Quote__c,o.Opportunity_Number__c,
                                        o.Quote_Cart_Number__c,o.Integration_Error_Message__c,o.Quote_Order_Type__c, o.Sales_Force__c, o.Sales_Channel__c,Partner__r.Partner_Quoting_Account_SAP_UCID__c,o.Tier_2_Partner__r.Partner_Quoting_Account_SAP_UCID__c, o.primary_alliance_partner__r.UCID__c, o.secondary_alliance_partner__r.UCID__c, o.oem_partner__r.UCID__c, o.Service_Provider__r.UCID__c, o.Account.UCID__c, o.Opportunity_Owner__c from Opportunity o where Id =: currentOppty.Id];
              }
           }
       } catch(QueryException e) { 
        ApexPages.addMessages(e);
       }
        lstErrMsgs = new List<String>();
        //Check User access on Opportunity
        if(!checkOpportunityAccess()){
            lstErrMsgs.add(System.Label.PROPEL_User_need_Edit_access);
            showBackLink = true;
        }
        //Validate that Sales Channel and Sales Force are populated
        if(quriedOppty.Sales_Channel__c == null || quriedOppty.Sales_Channel__c == '' ){
            lstErrMsgs.add(System.Label.PROPEL_SalesChannel_Required);
            showBackLink = true;
        }

         if(quriedOppty.Sales_Force__c == null || quriedOppty.Sales_Force__c == '' ){
            lstErrMsgs.add(System.Label.PROPEL_SalesForce_Required);
            showBackLink = true;
        }
        //Show Exception if Quote Number is already populated
        if(quriedOppty.Quote_Cart_Number__c != null ){
            lstErrMsgs.add(System.Label.PROPEL_Duplicate_Quote);
            showBackLink = true;
        }
        //Fix Defect 24296 : If the Opportunity_Owner__c is in the Custom setting HouseAccount__c.House_Account_User__c, then throw validation error message:
        String houseAccount = HouseAccount__c.getInstance().House_Account_User__c;
        if(quriedOppty.Opportunity_Owner__c != null && quriedOppty.Opportunity_Owner__c == houseAccount){
            lstErrMsgs.add(System.Label.PROPEL_HouseAccount_Err_Msg);
            showBackLink = true;
        }
        //Fix Defect 24296 : END
        
        //Return errors if validation fails
        if(!lstErrMsgs.isEmpty()){
            for(String errMsg : lstErrMsgs){
                ApexPages.addMessage(new ApexPages.message(ApexPages.severity.ERROR,errMsg));
            }
            return null;
        }
        //Prepare Hybris URL
        String hybrisURL = gCS.Hybris_URL__c;
        //Prepare Query String
        //Prepare encrypted 'distributionChannel' parameter to send as query string.
        if(quriedOppty.Sales_Channel__c != null && quriedOppty.Sales_Channel__c != ''){
            if(quriedOppty.Sales_Channel__c == gCS.Direct_Distribution_Channels__c){
                addQueryString(hybrisUrlParamNames.distributionChannel__c,quriedOppty.Sales_Channel__c);
            }
        }
        
        //Prepare encrypted 'quoteName' parameter to send as query string.
        if(quriedOppty.Name != null && quriedOppty.Name != ''){
            addQueryString(hybrisUrlParamNames.quoteName__c,quriedOppty.Name);
        }
        
         //Prepare encrypted 'account' parameter to send as query string.
        if(quriedOppty.Account.UCID__c != null && quriedOppty.Account.UCID__c != ''){
            addQueryString(hybrisUrlParamNames.account__c,quriedOppty.Account.UCID__c);
        }
        
          //Prepare encrypted 'tier1' parameter to send as query string.
        if(quriedOppty.Partner__r.Partner_Quoting_Account_SAP_UCID__c != null && quriedOppty.Partner__r.Partner_Quoting_Account_SAP_UCID__c != '' ){
            addQueryString(hybrisUrlParamNames.tier1__c,quriedOppty.Partner__r.Partner_Quoting_Account_SAP_UCID__c);
        }
        
          //Prepare encrypted 'tier2' parameter to send as query string.
        if(quriedOppty.Tier_2_Partner__r.Partner_Quoting_Account_SAP_UCID__c != null && quriedOppty.Tier_2_Partner__r.Partner_Quoting_Account_SAP_UCID__c != ''){
            addQueryString(hybrisUrlParamNames.tier2__c,quriedOppty.Tier_2_Partner__r.Partner_Quoting_Account_SAP_UCID__c);
        }
        
        //Prepare encrypted 'opportunityNumber' parameter to send as query string.
        if(quriedOppty.Opportunity_Number__c != null && quriedOppty.Opportunity_Number__c != ''){
            addQueryString(hybrisUrlParamNames.opportunityNumber__c,quriedOppty.Opportunity_Number__c);
        }
   
          //Prepare encrypted 'primaryAlliancePartner' parameter to send as query string.
        if(quriedOppty.primary_alliance_partner__r.UCID__c != null && quriedOppty.primary_alliance_partner__r.UCID__c != ''){// TO-DO : Create Account
            addQueryString(hybrisUrlParamNames.primaryAlliancePartner__c,quriedOppty.primary_alliance_partner__r.UCID__c);
        }
        
          //Prepare encrypted 'secondaryAlliancePartner' parameter to send as query string.
        if(quriedOppty.secondary_alliance_partner__r.UCID__c != null && quriedOppty.secondary_alliance_partner__r.UCID__c != ''){
            addQueryString(hybrisUrlParamNames.secondaryAlliancePartner__c,quriedOppty.secondary_alliance_partner__r.UCID__c);
        }
        
          //Prepare encrypted 'oemPartner' parameter to send as query string.
        if(quriedOppty.oem_partner__r.UCID__c != null && quriedOppty.oem_partner__r.UCID__c != ''){
            addQueryString(hybrisUrlParamNames.oemPartner__c,quriedOppty.oem_partner__r.UCID__c);
        }
        
          //Prepare encrypted 'servicePartner' parameter to send as query string.
        if(quriedOppty.Service_Provider__r.UCID__c != null && quriedOppty.Service_Provider__r.UCID__c != ''){// TO-DO : Create Account
            addQueryString(hybrisUrlParamNames.servicePartner__c,quriedOppty.Service_Provider__r.UCID__c);
        }
        
          //Prepare encrypted 'quoteSource' parameter to send as query string.(If 'Sales_Force__c' value is 'ESG' then send 'IIG'
        if(quriedOppty.Sales_Force__c  != null && quriedOppty.Sales_Force__c != ''){
                if(quriedOppty.Sales_Force__c == 'ESG')
                    addQueryString(hybrisUrlParamNames.quoteSource__c,'IIG');
                else
                    addQueryString(hybrisUrlParamNames.quoteSource__c,quriedOppty.Sales_Force__c);
        }
        
        queryString+='&' + 'iv' + '=' + ivb64;
        //Bhanu : PROPEL Commented to fix Defect #32260
        //queryString+= '&site=propel'; 
        PageReference redirectPage = new PageReference(hybrisURL + queryString);
        return redirectPage;
    }
   /*
   *   Method to return to Opportunity
   */
    public PageReference redirectToOppty(){
        return new PageReference('/'+quriedOppty.Id);
    }
    /*
    *  Check current user has access with the Opportunity
    */
    private  Boolean checkOpportunityAccess(){
        List<UserRecordAccess> lstURA = [SELECT RecordId FROM UserRecordAccess WHERE HasEditAccess = true AND UserId =:UserInfo.getUserId() AND RecordId=:quriedOppty.Id];
        if(lstURA != null && !lstURA.isEmpty()){
            return true;
        }
        return false;
    }
    /*
    *   Encrypts value passed
    */
    private String encriptValue(String value){
        // key should be in a custom setting
        if(createQuoteEncryptKey != null && createQuoteEncryptKey != ''){
            key = EncodingUtil.base64Decode(createQuoteEncryptKey);
            dataValue = Blob.valueOf(value);
            encrypted = Crypto.encrypt('AES128', key, ivBinary, dataValue);
            encryptedb64 = EncodingUtil.base64Encode(encrypted); // this is the parameter value that is sent to Hybris
        }
    return encryptedb64;
    }
    /*
    * Generates 16 digit random string
    */
    public String generateRandomString(Integer len){
        final string chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz';
        String randStr = '';
        while(randStr.length()<len){
            Integer idx = Math.mod(Math.abs(Crypto.getRandomInteger()), 62);
            randStr+= chars.substring(idx, idx+1);
        }
        return randStr;
    }
    /*
    * Checks and adds new query string
    */
    public void addQueryString(String name, String value){
        if(queryString.endsWith('?'))
                queryString+= name + '=' + EncodingUtil.urlEncode(encriptValue(value), 'UTF-8');
            else
                queryString+='&' + name + '=' + EncodingUtil.urlEncode(encriptValue(value), 'UTF-8');
    }
    /*
    * Custom exception class
    */
    private class CreateQuoteException extends Exception{}
   }