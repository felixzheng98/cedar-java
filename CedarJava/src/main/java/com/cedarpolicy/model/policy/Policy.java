/*
 * Copyright 2022-2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cedarpolicy.model.policy;

import com.cedarpolicy.loader.LibraryLoader;
import com.cedarpolicy.model.exception.InternalException;
import com.cedarpolicy.value.EntityUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.concurrent.atomic.AtomicInteger;


/** Policies in the Cedar language. */
public class Policy {
    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);
    static {
        LibraryLoader.loadLibrary();
    }

    /** Policy string. */
    public final String policySrc;
    /** Policy ID. */
    public final String policyID;

    /**
     * Creates a Cedar policy object.
     *
     * @param policy String containing the source code of a Cedar policy in the Cedar policy
     *     language.
     * @param policyID The id of this policy. Must be unique. Note: We may flip the order of the
     *     arguments here for idiomatic reasons.
     */
    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public Policy(
            @JsonProperty("policySrc") String policy, @JsonProperty("policyID") String policyID)
            throws NullPointerException {

        if (policy == null) {
            throw new NullPointerException("Failed to construct policy from null string");
        }
        if (policyID == null) {
            policyID = "policy" + ID_COUNTER.addAndGet(1);
        }
        this.policySrc = policy;
        this.policyID = policyID;
    }

    /**
     * Get the policy ID.
     */
    public String getID() {
        return policyID;
    }

    /**
     * Get the policy source.
     */
    public String getSource() {
        return policySrc;
    }

    @Override
    public String toString() {
        return "// Policy ID: " + policyID + "\n" + policySrc;
    }

    public static Policy parseStaticPolicy(String policyStr) throws InternalException, NullPointerException {
        var policyText = parsePolicyJni(policyStr);
        return new Policy(policyText, null);
    }

    public static Policy parsePolicyTemplate(String templateStr)  throws InternalException, NullPointerException {
        var templateText = parsePolicyTemplateJni(templateStr);
        return new Policy(templateText, null);
    }

    /**
     * This method takes in a template and a list of link values and calls Cedar JNI to ensure those slots
     * can be used to link the template. If the template is validated ahead of time by using Policy.parsePolicyTemplate
     * and the link values are also ensured to be valid (for example, by validating their parts using EntityTypeName.parse
     * and EntityIdentifier.parse), then this should only fail because the slots in the template don't match the link values
     * (barring JNI failures).
     * @param p Policy object constructed from a valid template. Best if built from Policy.parsePolicyTemplate
     * @param principal EntityUid to put into the principal slot. Leave null if there's no principal slot
     * @param resource EntityUid to put into the resource slot. Leave null if there's no resource slot
     * @return
     */
    public static boolean validateTemplateLinkedPolicy(Policy p, EntityUID principal, EntityUID resource)
            throws InternalException, NullPointerException {
        return validateTemplateLinkedPolicyJni(p.policySrc, principal, resource);
    }

    private static native String parsePolicyJni(String policyStr) throws InternalException, NullPointerException;
    private static native String parsePolicyTemplateJni(String policyTemplateStr)
            throws InternalException, NullPointerException;
    private static native boolean validateTemplateLinkedPolicyJni(String templateText, EntityUID principal, EntityUID resource)
            throws InternalException, NullPointerException;
}
