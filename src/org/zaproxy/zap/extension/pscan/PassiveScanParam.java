/*
 * Zed Attack Proxy (ZAP) and its related class files.
 * 
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 * 
 * Copyright 2012 The ZAP Development Team
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package org.zaproxy.zap.extension.pscan;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;
import org.parosproxy.paros.common.AbstractParam;
import org.zaproxy.zap.extension.api.ZapApiIgnore;
import org.zaproxy.zap.extension.pscan.scanner.RegexAutoTagScanner;


public class PassiveScanParam extends AbstractParam {
    
    private static final Logger logger = Logger.getLogger(PassiveScanParam.class);

    static final String PASSIVE_SCANS_BASE_KEY = "pscans";
    private static final String ALL_AUTO_TAG_SCANNERS_KEY = PASSIVE_SCANS_BASE_KEY + ".autoTagScanners.scanner";

    private static final String AUTO_TAG_SCANNER_NAME_KEY = "name";
    private static final String AUTO_TAG_SCANNER_TYPE_KEY = "type";
    private static final String AUTO_TAG_SCANNER_CONFIG_KEY = "config";
    private static final String AUTO_TAG_SCANNER_REQ_URL_REGEX_KEY = "reqUrlRegex";
    private static final String AUTO_TAG_SCANNER_REQ_HEAD_REGEX_KEY = "reqHeadRegex";
    private static final String AUTO_TAG_SCANNER_RES_HEAD_REGEX_KEY = "resHeadRegex";
    private static final String AUTO_TAG_SCANNER_RES_BODY_REGEX_KEY = "resBodyRegex";
    private static final String AUTO_TAG_SCANNER_ENABLED_KEY = "enabled";
    
    private static final String CONFIRM_REMOVE_AUTO_TAG_SCANNER_KEY = PASSIVE_SCANS_BASE_KEY + ".confirmRemoveAutoTagScanner";

    private static final String SCAN_ONLY_IN_SCOPE_KEY = PASSIVE_SCANS_BASE_KEY + ".scanOnlyInScope";

    private List<RegexAutoTagScanner> autoTagScanners = new ArrayList<>(0);
    
    private boolean confirmRemoveAutoTagScanner = true;

    /**
     * Flag that indicates whether or not the passive scan should be performed only on messages that are in scope.
     * <p>
     * Default is {@code false}, all messages are scanned.
     */
    private boolean scanOnlyInScope;
    
    public PassiveScanParam() {
    }
    
    @Override
    protected void parse() {
        try {
            List<HierarchicalConfiguration> fields = ((HierarchicalConfiguration) getConfig()).configurationsAt(ALL_AUTO_TAG_SCANNERS_KEY);
            this.autoTagScanners = new ArrayList<>(fields.size());
            List<String> tempListNames = new ArrayList<>(fields.size());
            for (HierarchicalConfiguration sub : fields) {
                String name = sub.getString(AUTO_TAG_SCANNER_NAME_KEY, "");
                if (!"".equals(name) && !tempListNames.contains(name)) {
                    tempListNames.add(name);
                    
                    RegexAutoTagScanner app = new RegexAutoTagScanner(
                        sub.getString(AUTO_TAG_SCANNER_NAME_KEY),
                        RegexAutoTagScanner.TYPE.valueOf(sub.getString(AUTO_TAG_SCANNER_TYPE_KEY)),
                        sub.getString(AUTO_TAG_SCANNER_CONFIG_KEY),
                        sub.getString(AUTO_TAG_SCANNER_REQ_URL_REGEX_KEY),
                        sub.getString(AUTO_TAG_SCANNER_REQ_HEAD_REGEX_KEY),
                        sub.getString(AUTO_TAG_SCANNER_RES_HEAD_REGEX_KEY),
                        sub.getString(AUTO_TAG_SCANNER_RES_BODY_REGEX_KEY),
                        sub.getBoolean(AUTO_TAG_SCANNER_ENABLED_KEY, true));
                    
                    autoTagScanners.add(app);
                }
            }
        } catch (ConversionException e) {
            logger.error("Error while loading the auto tag scanners: " + e.getMessage(), e);
        }

        this.confirmRemoveAutoTagScanner = getBoolean(CONFIRM_REMOVE_AUTO_TAG_SCANNER_KEY, true);
        this.scanOnlyInScope = getBoolean(SCAN_ONLY_IN_SCOPE_KEY, false);
    }

    public void setAutoTagScanners(List<RegexAutoTagScanner> scanners) {
        this.autoTagScanners = scanners;
        
        ((HierarchicalConfiguration) getConfig()).clearTree(ALL_AUTO_TAG_SCANNERS_KEY);

        for (int i = 0, size = scanners.size(); i < size; ++i) {
            String elementBaseKey = ALL_AUTO_TAG_SCANNERS_KEY + "(" + i + ").";
            RegexAutoTagScanner scanner = scanners.get(i);

            getConfig().setProperty(elementBaseKey + AUTO_TAG_SCANNER_NAME_KEY, scanner.getName());
            getConfig().setProperty(elementBaseKey + AUTO_TAG_SCANNER_TYPE_KEY, scanner.getType().toString());
            getConfig().setProperty(elementBaseKey + AUTO_TAG_SCANNER_CONFIG_KEY, scanner.getConf());
            getConfig().setProperty(elementBaseKey + AUTO_TAG_SCANNER_REQ_URL_REGEX_KEY, scanner.getRequestUrlRegex());
            getConfig().setProperty(elementBaseKey + AUTO_TAG_SCANNER_REQ_HEAD_REGEX_KEY, scanner.getRequestHeaderRegex());
            getConfig().setProperty(elementBaseKey + AUTO_TAG_SCANNER_RES_HEAD_REGEX_KEY, scanner.getResponseHeaderRegex());
            getConfig().setProperty(elementBaseKey + AUTO_TAG_SCANNER_RES_BODY_REGEX_KEY, scanner.getResponseBodyRegex());
            getConfig().setProperty(elementBaseKey + AUTO_TAG_SCANNER_ENABLED_KEY, scanner.isEnabled());
        }
    }
    
    public List<RegexAutoTagScanner> getAutoTagScanners() {
        return autoTagScanners;
    }
    
    @ZapApiIgnore
    public boolean isConfirmRemoveAutoTagScanner() {
        return this.confirmRemoveAutoTagScanner;
    }
    
    @ZapApiIgnore
    public void setConfirmRemoveAutoTagScanner(boolean confirmRemove) {
        this.confirmRemoveAutoTagScanner = confirmRemove;
        getConfig().setProperty(CONFIRM_REMOVE_AUTO_TAG_SCANNER_KEY, confirmRemoveAutoTagScanner);
    }
    
    /**
     * Sets whether or not the passive scan should be performed only on messages that are in scope.
     *
     * @param scanOnlyInScope {@code true} if the scan should be performed only on messages that are in scope, {@code false}
     *            otherwise.
     * @since 2.6.0
     * @see #isScanOnlyInScope()
     * @see org.parosproxy.paros.model.Session#isInScope(String) Session.isInScope(String)
     */
    public void setScanOnlyInScope(boolean scanOnlyInScope) {
        this.scanOnlyInScope = scanOnlyInScope;
        getConfig().setProperty(SCAN_ONLY_IN_SCOPE_KEY, scanOnlyInScope);
    }

    /**
     * Tells whether or not the passive scan should be performed only on messages that are in scope.
     *
     * @return {@code true} if the scan should be performed only on messages that are in scope, {@code false} otherwise.
     * @since 2.6.0
     * @see #setScanOnlyInScope(boolean)
     */
    public boolean isScanOnlyInScope() {
        return scanOnlyInScope;
    }
}
