package io.github.navms.agent;

import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.permission.PermissionRule;

import java.util.List;

/**
 * Permission configuration.
 *
 * @author navms
 */
public interface Permissions {

    static PermissionContextState bypassPermissions() {
        return PermissionContextState.builder().mode(PermissionMode.BYPASS).build();
    }

    static PermissionContextState askPermissions(List<String> toolNames) {
        PermissionContextState.Builder builder = PermissionContextState.builder().mode(PermissionMode.BYPASS);
        for (String toolName : toolNames) {
            builder.addAskRule(
                    toolName,
                    new PermissionRule(toolName, null, PermissionBehavior.ASK, "policy"));
        }
        return builder.build();
    }

}
