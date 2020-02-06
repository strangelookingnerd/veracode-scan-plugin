/*******************************************************************************
 * Copyright (c) 2014 Veracode, Inc. All rights observed.
 *
 * Available for use by Veracode customers as described in the accompanying license agreement.
 *
 * Send bug reports or enhancement requests to support@veracode.com.
 *
 * See the license agreement for conditions on submitted materials.
 ******************************************************************************/

package io.jenkins.plugins.veracode.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import com.veracode.apiwrapper.cli.VeracodeCommand.VeracodeParser;
import io.jenkins.plugins.veracode.args.GetAppListArgs;
import io.jenkins.plugins.veracode.data.ProxyBlock;

import hudson.util.FormValidation;
import hudson.util.Secret;

/**
 * A utility class for performing validation of form fields.
 *
 *
 */
public final class FormValidationUtil {

	private static final String DEFAULT_TIMEOUT = "60";
	private static final String ANALYSIS_NAME_DISPLAY_TEXT = "analysis name";
	private static final String ANALYSIS_NAME_REQUIRED_DISPLAY_TEXT = "Analysis Name";
	private static final int MINIMUM_LENGTH_FOR_DA_ANALYSIS_NAME = 6;
	private static final int MAXIMUM_LENGTH_FOR_DA_ANALYSIS_NAME = 190;
	private static final String DA_MAX_DURATION_DISPLAY_TEXT = "maximum duration";
	private static final String DA_MAX_RESULTS_DURATION_DISPLAY_TEXT = "wait for results duration";
	private static final int MINIMUM_VALUE_DA_MAX_DURATION_HOURS = 1; // Hours
	private static final int MAXIMUM_VALUE_DA_MAX_DURATION_HOURS = 600; // Hours
	private static final int MINIMUM_VALUE_DA_RESULTS_DURATION_HOURS = 0; // Hours
	private static final int MAXIMUM_VALUE_DA_RESULTS_DURATION_HOURS = 600; // Hours
	private static final String VID_DISPLAY_TEXT = "API ID";
	private static final String VKEY_DISPLAY_TEXT = "API Key";
	private static final String VID_IHELP_TEXT = "ID";
	private static final String VKEY_IHELP_TEXT = "key";

	/**
	 * Determines the result of the form field validation depending on whether
	 * two mutually inclusive form fields have user-supplied data.
	 *
	 *
	 * @param fieldValue String
	 * @param dependencyFieldValue String
	 * @param fieldDisplayName String
	 * @param dependencyFieldDisplayName String
	 * @param dependencyFieldIHelpTextName String
	 * @return FormValidation
	 */
	public static FormValidation checkMutuallyInclusiveFields(String fieldValue, String dependencyFieldValue, String fieldDisplayName, String dependencyFieldDisplayName,
			String dependencyFieldIHelpTextName) {
		if (StringUtil.isNullOrEmpty(fieldValue) && !StringUtil.isNullOrEmpty(dependencyFieldValue)) {
			return FormValidation.error(String.format("%s is required.", fieldDisplayName));
		} else if (!StringUtil.isNullOrEmpty(fieldValue) && StringUtil.isNullOrEmpty(dependencyFieldValue)) {
			if (dependencyFieldDisplayName.equals(VID_DISPLAY_TEXT) || dependencyFieldDisplayName.equals(VKEY_DISPLAY_TEXT)) {
				return FormValidation.warning(String.format("After entering your %s, you must also enter your %s in the %s field.", fieldDisplayName, dependencyFieldIHelpTextName,
						dependencyFieldDisplayName));
			} else {
				return FormValidation.warning(String.format("If %s is provided, %s must also be provided.", fieldDisplayName, dependencyFieldDisplayName));
			}
		} else {
			return FormValidation.ok();
		}
	}

	/**
	 * Determines the result of the form field validation depending on whether
	 * two form fields have user-supplied data.
	 *
	 *
	 * @param fieldValue String
	 * @param dependencyFieldValue String
	 * @param fieldDisplayName String
	 * @param dependencyFieldDisplayName String
	 * @param dependencyFieldIHelpTextName String
	 * @return FormValidation
	 */
	public static FormValidation checkFields(String fieldValue, String dependencyFieldValue, String fieldDisplayName, String dependencyFieldDisplayName,
			String dependencyFieldIHelpTextName) {
		if (StringUtil.isNullOrEmpty(fieldValue)) {
			return FormValidation.error(String.format("%s is required.", fieldDisplayName));
		} else if (!StringUtil.isNullOrEmpty(fieldValue) && StringUtil.isNullOrEmpty(dependencyFieldValue)) {
			if (dependencyFieldDisplayName.equals(VID_DISPLAY_TEXT) || dependencyFieldDisplayName.equals(VKEY_DISPLAY_TEXT)) {
				return FormValidation.warning(String.format("After entering your %s, you must also enter your %s in the %s field.", fieldDisplayName, dependencyFieldIHelpTextName,
						dependencyFieldDisplayName));
			} else {
				return FormValidation.warning(String.format("If %s is provided, %s must also be provided.", fieldDisplayName, dependencyFieldDisplayName));
			}
		} else {
			return FormValidation.ok();
		}
	}

	/**
	 * Determines the result of the form field validation depending on whether a
	 * web request to Veracode's getapplist.do's API end point with the supplied
	 * credentials and proxy settings is successful.
	 *
	 * {@link hudson.util.FormValidation FormValidation}
	 * {@link org.kohsuke.stapler.bind.JavaScriptMethod JavaScriptMethod}
	 *
	 * @param id String
	 * @param key String
	 * @param proxy boolean
	 * @return FormValidation
	 */
	public static FormValidation checkConnection(String id, String key, ProxyBlock proxy) {
		try {
			PrintStream ps = null;
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ps = new PrintStream(baos, true, "UTF-8");

				VeracodeParser parser = new VeracodeParser();
				parser.throwExceptions(true);
				parser.setOutputWriter(null);
				parser.setErrorWriter(ps);

				GetAppListArgs appListArgs = GetAppListArgs.newGetAppListArgs(id, key, proxy);

				parser.parse(appListArgs.getArguments());

				//assumes that if the wrapper wrote to the error stream, there was an error
				String errorText = baos.toString("UTF-8");
				return !StringUtil.isNullOrEmpty(errorText) ? FormValidation.error(errorText) : FormValidation.ok("Success!");
			} finally {
				if (ps != null) {
					ps.close();
				}
			}
		} catch (Throwable e) {
			return FormValidation.error(getApiWrapperExceptionMessage(e));
		}
	}

	/**
	 * Check if analysis name is empty
	 * @param analysisName String
	 * @return FormValidation
	 */
	public static FormValidation checkAnalysisName(String analysisName) {
		if (StringUtil.isNullOrEmpty(analysisName)) {
			return FormValidation.error(String.format("%s is required.", ANALYSIS_NAME_REQUIRED_DISPLAY_TEXT));
		}
		int textLength = analysisName.trim().length();
		if (textLength < MINIMUM_LENGTH_FOR_DA_ANALYSIS_NAME || textLength > MAXIMUM_LENGTH_FOR_DA_ANALYSIS_NAME) {
			return FormValidation.error(String.format("Enter an %s of %s-%s characters.",
					ANALYSIS_NAME_DISPLAY_TEXT, MINIMUM_LENGTH_FOR_DA_ANALYSIS_NAME, MAXIMUM_LENGTH_FOR_DA_ANALYSIS_NAME));
		}
		return FormValidation.ok();
	}

	/**
	 * Returns error message if entered value is less than minimum number,
	 * or greater than maximum number, or not a valid number.
	 * @param maximumDuration String
	 * @return FormValidation
	 */
	public static FormValidation checkMaximumDuration(String maximumDuration) {
		if (!StringUtil.isNullOrEmpty(maximumDuration)) {
			try {
				int mxDurValue = Integer.parseInt(maximumDuration);
				return checkMaximumDuration(mxDurValue) != null
						? FormValidation.error(checkMaximumDuration(mxDurValue))
						: FormValidation.ok();
			} catch (NumberFormatException nfe) {
				return FormValidation.error(String.format("%s is not a valid number.", maximumDuration));
			}
		}
		return FormValidation.ok();
	}

	/**
	 * Returns error message if entered value is less than minimum number,
	 * or greater than maximum number.
	 * @param maximumDuration int
	 * @return String
	 */
	public static String checkMaximumDuration(int maximumDuration) {
		if (maximumDuration < MINIMUM_VALUE_DA_MAX_DURATION_HOURS || maximumDuration > MAXIMUM_VALUE_DA_MAX_DURATION_HOURS) {
			return String.format("Enter a %s of %s-%s hours.",
					DA_MAX_DURATION_DISPLAY_TEXT, MINIMUM_VALUE_DA_MAX_DURATION_HOURS, MAXIMUM_VALUE_DA_MAX_DURATION_HOURS);
		}
		return null;
	}

	/**
	 * Returns error message if entered value is less than minimum number,
	 * or greater than maximum number, or not a valid number.
	 * @param waitForResultsDuration String
	 * @return FormValidation
	 */
	public static FormValidation checkWaitForResultsDuration(String waitForResultsDuration) {
		if (!StringUtil.isNullOrEmpty(waitForResultsDuration)) {
			try {
				int resultsDurValue = Integer.parseInt(waitForResultsDuration);
				return checkWaitForResultsDuration(resultsDurValue) != null
						? FormValidation.error(checkWaitForResultsDuration(resultsDurValue))
						: FormValidation.ok();
			} catch (NumberFormatException nfe) {
				return FormValidation.error(String.format("%s is not a valid number.", waitForResultsDuration));
			}
		}
		return FormValidation.ok();
	}

	/**
	 * Returns error message if entered value is less than minimum number,
	 * or greater than maximum number.
	 * @param waitForResultsDuration int
	 * @return String
	 */
	public static String checkWaitForResultsDuration(int waitForResultsDuration) {
		if (waitForResultsDuration < MINIMUM_VALUE_DA_RESULTS_DURATION_HOURS || waitForResultsDuration > MAXIMUM_VALUE_DA_RESULTS_DURATION_HOURS) {
			return String.format("Enter a %s of up to %s hours (25 days).",
					DA_MAX_RESULTS_DURATION_DISPLAY_TEXT, MAXIMUM_VALUE_DA_RESULTS_DURATION_HOURS);
		}
		return null;
	}

	/**
	 * Check if API ID and dependency fields are entered
	 * @param vid String
	 * @param vkey String
	 * @param hasGlobalApiIdKeyCredentials boolean
	 * @return FormValidation
	 */
	public static FormValidation checkApiId(String vid, String vkey, boolean hasGlobalApiIdKeyCredentials) {
		if (hasGlobalApiIdKeyCredentials) {
			if (!StringUtil.isNullOrEmpty(vid) && !StringUtil.isNullOrEmpty(vkey)) {
				return FormValidation.warning("These Veracode API credentials override the global Veracode API credentials.");
			} else if (!(StringUtil.isNullOrEmpty(vid) && StringUtil.isNullOrEmpty(vkey))) {
				return FormValidationUtil.checkFields(vid, vkey, VID_DISPLAY_TEXT, VKEY_DISPLAY_TEXT, VKEY_DISPLAY_TEXT);
			} else {
				return FormValidationUtil.checkMutuallyInclusiveFields(vid, vkey, VID_DISPLAY_TEXT, VKEY_IHELP_TEXT, VKEY_DISPLAY_TEXT);
			}
		} else {
			return FormValidationUtil.checkFields(vid, vkey, VID_DISPLAY_TEXT, VKEY_DISPLAY_TEXT, VKEY_DISPLAY_TEXT);
		}
	}

	/**
	 * Check if API Key and dependency fields are entered
	 * @param vid String
	 * @param vkey String
	 * @param hasGlobalApiIdKeyCredentials boolean
	 * @return FormValidation
	 */
	public static FormValidation checkApiKey(String vid, String vkey, boolean hasGlobalApiIdKeyCredentials) {
		if (hasGlobalApiIdKeyCredentials) {
			if (!StringUtil.isNullOrEmpty(vid) && !StringUtil.isNullOrEmpty(vkey)) {
				return FormValidation.warning("These Veracode API credentials override the global Veracode API credentials.");
			} else if (!(StringUtil.isNullOrEmpty(vid) && StringUtil.isNullOrEmpty(vkey))) {
				return FormValidationUtil.checkFields(vkey, vid, VKEY_DISPLAY_TEXT, VID_DISPLAY_TEXT, VID_DISPLAY_TEXT);
			} else {
				return FormValidationUtil.checkMutuallyInclusiveFields(vkey, vid, VKEY_DISPLAY_TEXT, VID_IHELP_TEXT, VID_DISPLAY_TEXT);
			}
		} else {
			return FormValidationUtil.checkFields(vkey, vid, VKEY_DISPLAY_TEXT, VID_DISPLAY_TEXT, VID_DISPLAY_TEXT);
		}
	}

	/**
	 * Returns a printable exception message for exceptions thrown by the Api
	 * Wrapper. If the {@link Throwable} passed to this method is of type
	 * {@link RuntimeException} and if a "cause" is present the error message
	 * returned by this method would contain information about that the "cause"
	 * exception. (When the wrapper runs in "throwExceptions" mode and it
	 * encounters an exception [or error], rather than attempt to process it it
	 * re-throws it as the 'cause' of a {@link RuntimeException}).
	 *
	 * @param e Throwable
	 * @return String
	 */
	private static String getApiWrapperExceptionMessage(Throwable e) {
		if (e instanceof RuntimeException && e.getCause() != null) {
			e = e.getCause();
		}
		return e != null ? String.format("%s: %s", e.getClass().getName(), StringUtil.getEmptyIfNull(e.getMessage())) : "";
	}

	/**
	 * Assign a default timeout values if waitForScan is set to true and no timeout value entered
	 * @param timeoutValue String
	 * @return String str_timeout
	 */
	public static String formatTimeout(final String timeoutValue) {
		String str_timeout;
		if(!StringUtil.isNullOrEmpty(Secret.toString(Secret.fromString(timeoutValue))) && Integer.parseInt(Secret.toString(Secret.fromString(timeoutValue))) >= 0) {
			str_timeout = timeoutValue;
		} else {
			str_timeout = DEFAULT_TIMEOUT;
		}
		return str_timeout;
	}

	private FormValidationUtil(){}
}
