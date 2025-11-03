# Nafath Custom Authenticator for WSO2 Identity Server

## Overview

The **Nafath Custom Authenticator** enables integration between **WSO2 Identity Server (WSO2 IS)** and **Nafath**, the Saudi Arabian national digital identity verification platform. This integration allows users to authenticate securely using their **Nafath credentials**, aligning with **KSA’s digital transformation and cybersecurity standards**.

By integrating Nafath with WSO2 IS, organizations can provide **seamless, passwordless, and secure login experiences** for various digital services — including government, financial, healthcare, education, and enterprise systems.

---

## Table of Contents

* [Key Features](#key-features)
* [Prerequisites](#prerequisites)
* [Building the Connector](#building-the-connector)
* [Deploying the Connector](#deploying-the-connector)
* [Configuring Nafath Authenticator in WSO2 IS](#configuring-nafath-authenticator-in-wso2-is)

  * [For WSO2 IS 7.0.0 and Later](#for-wso2-is-700-and-later)
  * [For Older Versions](#for-older-versions)
* [Updating the Nafath Logo (Optional)](#updating-the-nafath-logo-optional)
* [Configuring Applications](#configuring-applications)
* [Testing the Integration](#testing-the-integration)
* [Use Cases in the KSA Ecosystem](#use-cases-in-the-ksa-ecosystem)
* [References](#references)

---

## Key Features

* ✅ **Seamless Nafath authentication** through WSO2 IS
* 🔒 **Secure identity federation** with KSA’s national ID platform
* ⚙️ **Customizable authentication flow** (single-step or multi-step)
* 🧩 **Easy deployment** via drop-in JAR and JSP files
* 🌍 **Support for multiple WSO2 IS versions (5.11.0+ → 7.x)**
* 🖼️ **Custom branding support** (e.g., Nafath logo integration)

---

## Prerequisites

Before configuring the connector, ensure you have the following:

* **WSO2 Identity Server** (version **7.0.0** or later recommended)
* **Java 8** or higher
* **Maven** installed
* **Nafath API credentials** (SP API key and secret)
* Access to:

  * `<WSO2_IS_HOME>/repository/components/dropins`
  * `<WSO2_IS_HOME>/repository/deployment/server/webapps/authenticationendpoint`

---

## Building the Connector

1. Clone the Nafath connector project:

   ```bash
   git clone https://github.com/nsivanoly/nafath-authenticator.git
   cd nafath-authenticator
   ```

2. Build the project using Maven:

   ```bash
   mvn clean install
   ```

3. After a successful build, locate the generated `.jar` file inside the `target` directory.

---

## Deploying the Connector

1. **Stop the WSO2 Identity Server** (if running).

2. Copy the built JAR file to:

   ```
   <WSO2_IS_HOME>/repository/components/dropins/
   ```

3. Copy the UI pages (`nafath.jsp` and `nafath-random.jsp`) from:

   ```
   nafath-authenticator/src/main/resources/
   ```

   to:

   ```
   <WSO2_IS_HOME>/repository/deployment/server/webapps/authenticationendpoint/
   ```

4. **Restart WSO2 IS** to apply changes:

   ```bash
   # macOS/Linux
   sh <WSO2_IS_HOME>/bin/wso2server.sh

   # Windows
   <WSO2_IS_HOME>\bin\wso2server.bat
   ```

---

## Configuring Nafath Authenticator in WSO2 IS

### For WSO2 IS 7.0.0 and Later

1. Log in to the **WSO2 IS Console**.
2. Navigate to:
   `Connections` → `New Connection` → `Custom Connector`
3. Create a new connector and select **NafathCustomAuthenticator**.
4. Under the **Settings** tab, click **New Authenticator**.
5. Enter the following configuration details:

   * **SP API Key**
   * **SP API Secret**
6. Save the configuration.

---

### For Older Versions

1. Log in to the **WSO2 IS Management Console**.
2. Navigate to:
   `Identity Providers` → `Add`
3. Expand the **Federated Authenticators** section.
4. Select **Nafath Authenticator Configuration**.
5. Provide the **SP API Key** and **SP API Secret**.
6. Save the identity provider.

---

## Updating the Nafath Logo (Optional)

To brand the login page and console with the Nafath logo:

1. Copy the logo file:

   ```
   nafath-logo.png
   ```

   to the following locations:

   ```
   <WSO2_IS_HOME>/repository/deployment/server/webapps/authenticationendpoint/libs/themes/default/assets/images/identity-providers/
   <WSO2_IS_HOME>/repository/deployment/server/webapps/console/resources/connections/assets/images/logos/
   ```

2. Optionally, update the logo path via API:

   ```bash
   curl -k --location --request PATCH \
   'https://localhost:9443/t/carbon.super/api/server/v1/identity-providers/<idp-id>' \
   --header 'Authorization: Basic YWRtaW46YWRtaW4=' \
   --header 'Accept: application/json' \
   --header 'Content-Type: application/json' \
   --data '[
       {
           "operation": "REPLACE",
           "path": "/image",
           "value": "assets/images/logos/nafath-logo.png"
       }
   ]'
   ```

---

## Configuring Applications

1. Go to **Applications** in WSO2 IS.
2. Click **New Application** and configure the necessary details.
3. Under **Login Flow**, select **Nafath Authenticator**.
4. (Optional) Configure multi-step authentication if required.
5. Save and update the application.

---

## Testing the Integration

1. Access the application’s **login page**.
2. Select **Nafath Authentication**.
3. Enter the **Nafath ID**.
4. Approve the authentication request on the **Nafath mobile app**.
5. Verify that the login flow completes successfully.

---

## Use Cases in the KSA Ecosystem

* **Government Services:** Secure citizen access to portals like *Absher* and *Tawakkalna*.
* **Banking and Finance:** Strong customer authentication for digital banking.
* **Corporate Environments:** Single Sign-On (SSO) using Nafath for employee systems.
* **E-Commerce:** Identity verification to reduce fraud in online transactions.
* **Healthcare:** Secure patient access to health records.
* **Education:** Verified access for students and faculty in digital learning systems.
* **APIs and Microservices:** Federated authentication for protected APIs.

---

## References

* [WSO2 Identity Server 7.x Documentation](https://is.docs.wso2.com/en/latest/deploy/get-started/run-the-product/)
* [Nafath Official Portal (Saudi Digital Government Authority)](https://www.apd.gov.sa/en/signin/nafath)

---

**Author:** WSO2 Community / Custom Connector Team
**License:** Apache License 2.0