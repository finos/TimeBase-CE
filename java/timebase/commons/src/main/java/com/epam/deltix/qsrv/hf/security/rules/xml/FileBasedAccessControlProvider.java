package com.epam.deltix.qsrv.hf.security.rules.xml;

import com.epam.deltix.util.io.UncheckedIOException;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.security.*;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Can read AccessControlRules from XML file
 */
public class FileBasedAccessControlProvider implements AccessControlRulesFactory {

    public static final Logger LOGGER = Logger.getLogger("deltix.uac"); //TODO: Unify loggers
    private final File rulesFile;

    public FileBasedAccessControlProvider(File rulesFile) {
        this.rulesFile = rulesFile;
    }


    @Override
    public synchronized AccessControlRule[] create(UserDirectory userDirectory) {
        // Rules processing depend on current state of user directory and cannot be cached (unless we sure that user directory remains the same)
        try {
            return load(userDirectory, RulesConfiguration.read(rulesFile));
        } catch (JAXBException e) {
            throw new UncheckedIOException("(UAC) Rules reading failure: " + e.getMessage(), e);
        }
    }


    private static AccessControlRule [] load (UserDirectory userDirectory, RulesConfiguration rulesConfig) {
        List<AccessControlRule> result = new ArrayList<>();
        for (int i = 0; i < rulesConfig.rules.size(); i++) {
            RuleEntry rule = createRuleEntry(userDirectory, rulesConfig.rules.get(i));
            if (rule != null) {
                if ( ! rule.isEmpty())
                    result.addAll(rule.listAccessRules());
                else
                    LOGGER.warning("(UAC) Skipping empty rule: " + rule);
            }
        }
        return result.toArray(new AccessControlRule[result.size()]);
    }

    private static RuleEntry createRuleEntry(UserDirectory userDirectory, Rule rule) {
        String ruleId = Arrays.toString(rule.principals) + "." + Arrays.toString(rule.permissions) + (rule.resources != null ? "." + Arrays.toString(rule.resources) : "");
        RuleEntry result = new RuleEntry(ruleId);

        result.setEffect(rule instanceof AllowRule ? AccessControlRule.RuleEffect.Allow : AccessControlRule.RuleEffect.Deny);
        if (rule.principals != null) {
            for (int i = 0; i < rule.principals.length; i++) {
                String principalName = StringUtils.trim(rule.principals[i]);
                if (principalName == null) {
                    LOGGER.log(Level.WARNING, "[UAC] Skip empty principal for rule {0}", ruleId);
                    continue;
                }

                Principal principal = userDirectory.getUser(principalName);
                if (principal == null)
                    principal = userDirectory.getGroup(principalName);
                if (principal == null) {
                    LOGGER.log(Level.WARNING, "[UAC] Unknown rule principal {0} ({1})", new Object[]{principalName, ruleId});
                    continue;
                }
                result.addPrincipal(principal);
            }
        }

        if (rule.permissions != null) {
            for (int i = 0; i < rule.permissions.length; i++) {
                String permission = StringUtils.trim(rule.permissions[i]);
                if (permission == null) {
                    LOGGER.log(Level.WARNING, "[UAC] Skip empty permission for rule {0}", ruleId);
                    continue;
                }
                result.addPermission(permission);
            }
        }

        if (rule.resources != null) {
            for (int i = 0; i < rule.resources.length; i++) {
                Resource resource = rule.resources[i];
                String resourceName = StringUtils.trim(resource.value);
                if (resourceName == null) {
                    LOGGER.log(Level.WARNING, "[UAC] Skip resource with empty name for rule {0}", ruleId);
                    continue;
                }
                result.addResource(new ResourceEntry(resourceName, resource.type, resource.format));
            }
        }

        return result;
    }
}
