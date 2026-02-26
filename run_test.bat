@echo off
mvn test -DsuiteXmlFile=testng_coupon_member_nonmember_full.xml > straight_flow_validationn.log 2>&1
echo Exit Code: %ERRORLEVEL%
