/*
 * Copyright 2016 The OpenYOLO Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openyolo.spi;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import com.google.bbq.BaseBroadcastQueryReceiver;
import com.google.bbq.Protobufs.BroadcastQuery;
import com.google.bbq.QueryResponseSender;
import java.util.HashSet;
import java.util.Set;
import org.openyolo.protocol.AuthenticationDomain;
import org.openyolo.protocol.CredentialRetrieveRequest;
import org.openyolo.protocol.MalformedDataException;

/**
 * Partial implementation of an OpenYOLO request receiver, that should be extended by providers.
 * This implementation handles some basic validation and decoding of the request before
 * handing the request to
 * {@link #processCredentialRequest(Context, BroadcastQuery, CredentialRetrieveRequest, Set)}.
 */
public abstract class BaseCredentialQueryReceiver extends BaseBroadcastQueryReceiver {

    /**
     * Creates a receiver that will log errors to the specified log tag.
     */
    public BaseCredentialQueryReceiver(String logTag) {
        super(logTag);
    }

    @Override
    protected void processQuery(@NonNull Context context, @NonNull BroadcastQuery query) {

        CredentialRetrieveRequest request;
        try {
            byte[] encodedProto = query.getQueryMessage().toByteArray();
            request = CredentialRetrieveRequest.fromProtobufBytes(encodedProto);
        } catch (MalformedDataException ex) {
            Log.w(mLogTag, "Credential request message failed field validation", ex);
            new QueryResponseSender(context).sendResponse(query, null);
            return;
        }

        AuthenticationDomain requestorAuthDomain =
                AuthenticationDomain.fromPackageName(context, query.getRequestingApp());

        // Ensure the authentication domain of the requesting app can be determined
        if (null == requestorAuthDomain) {
            Log.w(mLogTag, "Unable to determine the authentication domain of the requesting app");
            new QueryResponseSender(context).sendResponse(query, null);
            return;
        }

        HashSet<AuthenticationDomain> authenticationDomains = new HashSet<>();
        authenticationDomains.add(requestorAuthDomain);
        processCredentialRequest(context, query, request, authenticationDomains);
    }

    protected abstract void processCredentialRequest(
            @NonNull Context context,
            @NonNull BroadcastQuery query,
            @NonNull CredentialRetrieveRequest request,
            @NonNull Set<AuthenticationDomain> requestorDomains);
}
