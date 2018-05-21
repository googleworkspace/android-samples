/**
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
// [START apps_script_execute]
/**
 * Call the API to run an Apps Script function that returns a list
 * of folders within the user's root directory on Drive.
 *
 * @return list of String folder names and their IDs
 * @throws IOException
 */
private List<String> getDataFromApi()
        throws IOException, GoogleAuthException {
    // ID of the script to call. Acquire this from the Apps Script editor,
    // under Publish > Deploy as API executable.
    String scriptId = "ENTER_YOUR_SCRIPT_ID_HERE";

    List<String> folderList = new ArrayList<String>();

    // Create an execution request object.
    ExecutionRequest request = new ExecutionRequest()
            .setFunction("getFoldersUnderRoot");

    // Make the request.
    Operation op =
            mService.scripts().run(scriptId, request).execute();

    // Print results of request.
    if (op.getError() != null) {
        throw new IOException(getScriptError(op));
    }
    if (op.getResponse() != null &&
            op.getResponse().get("result") != null) {
        // The result provided by the API needs to be cast into
        // the correct type, based upon what types the Apps Script
        // function returns. Here, the function returns an Apps
        // Script Object with String keys and values, so must be
        // cast into a Java Map (folderSet).
        Map<String, String> folderSet =
                (Map<String, String>)(op.getResponse().get("result"));

        for (String id: folderSet.keySet()) {
            folderList.add(
                    String.format("%s (%s)", folderSet.get(id), id));
        }
    }

    return folderList;
}

/**
 * Interpret an error response returned by the API and return a String
 * summary.
 *
 * @param op the Operation returning an error response
 * @return summary of error response, or null if Operation returned no
 *     error
 */
private String getScriptError(Operation op) {
    if (op.getError() == null) {
        return null;
    }

    // Extract the first (and only) set of error details and cast as a Map.
    // The values of this map are the script's 'errorMessage' and
    // 'errorType', and an array of stack trace elements (which also need to
    // be cast as Maps).
    Map<String, Object> detail = op.getError().getDetails().get(0);
    List<Map<String, Object>> stacktrace =
            (List<Map<String, Object>>)detail.get("scriptStackTraceElements");

    java.lang.StringBuilder sb =
            new StringBuilder("\nScript error message: ");
    sb.append(detail.get("errorMessage"));

    if (stacktrace != null) {
        // There may not be a stacktrace if the script didn't start
        // executing.
        sb.append("\nScript error stacktrace:");
        for (Map<String, Object> elem : stacktrace) {
            sb.append("\n  ");
            sb.append(elem.get("function"));
            sb.append(":");
            sb.append(elem.get("lineNumber"));
        }
    }
    sb.append("\n");
    return sb.toString();
}
// [END apps_script_execute]
