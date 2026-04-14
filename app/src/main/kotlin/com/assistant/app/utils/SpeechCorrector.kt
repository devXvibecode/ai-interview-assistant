package com.assistant.app.utils

object SpeechCorrector {

    private val corrections = mapOf(
        // Microsoft Products
        "conditional excess" to "conditional access",
        "conditional axis" to "conditional access",
        "conditional axes" to "conditional access",
        "in tune" to "Intune",
        "in-tune" to "Intune",
        "into me" to "Intune",
        "enter id" to "Entra ID",
        "intra id" to "Entra ID",
        "entrada id" to "Entra ID",
        "enter a id" to "Entra ID",
        "one drive" to "OneDrive",
        "share point" to "SharePoint",
        "share points" to "SharePoint",
        "auto pilot" to "Autopilot",
        "auto-pilot" to "Autopilot",
        "bit locker" to "BitLocker",
        "bit-locker" to "BitLocker",
        "file vault" to "FileVault",
        "file-vault" to "FileVault",
        "azure 80" to "Azure AD",
        "azure ad" to "Azure AD",
        "power apps" to "Power Apps",
        "power automate" to "Power Automate",
        "power bi" to "Power BI",

        // Email / DNS
        "d kim" to "DKIM",
        "dickem" to "DKIM",
        "d-kim" to "DKIM",
        "dkim record" to "DKIM record",
        "dmarc record" to "DMARC record",
        "de-marc" to "DMARC",
        "d marc" to "DMARC",
        "spf record" to "SPF record",
        "mx record" to "MX record",
        "mx records" to "MX records",

        // Security
        "zero scaler" to "Zscaler",
        "z scaler" to "Zscaler",
        "z-scaler" to "Zscaler",
        "james pro" to "JAMF Pro",
        "jam pro" to "JAMF Pro",
        "jamb pro" to "JAMF Pro",
        "jam f" to "JAMF",
        "siem" to "SIEM",
        "zepa" to "ZPA",
        "z p a" to "ZPA",
        "z i a" to "ZIA",
        "zed pa" to "ZPA",
        "zed ia" to "ZIA",
        "zt na" to "ZTNA",
        "zero trust network access" to "Zero Trust Network Access",
        "mf a" to "MFA",
        "m f a" to "MFA",
        "multi-factor authentication" to "Multi-Factor Authentication",
        "single sign on" to "Single Sign-On",
        "single sign-on" to "SSO",
        "s s o" to "SSO",
        "fido 2" to "FIDO2",
        "fido two" to "FIDO2",

        // Protocols / Tech
        "s c e p" to "SCEP",
        "p k c s" to "PKCS",
        "r b a c" to "RBAC",
        "r-bac" to "RBAC",
        "ldap" to "LDAP",
        "keberos" to "Kerberos",
        "kerbos" to "Kerberos",
        "active-directory" to "Active Directory",
        "a d d s" to "AD DS",
        "group-policy" to "Group Policy",
        "g p o" to "GPO",
        "p i m" to "PIM",
        "privileged identity management" to "Privileged Identity Management",
        "d e p" to "DEP",
        "a d e" to "ADE",
        "m d m" to "MDM",
        "m a m" to "MAM",
        "e d r" to "EDR",
        "d l p" to "DLP",
        "s i e m" to "SIEM",
        "e 3" to "E3",
        "e 5" to "E5",
        "e three" to "E3",
        "e five" to "E5",
        "microsoft 365" to "Microsoft 365",
        "office 365" to "Office 365",
        "m 365" to "M365",
        "exchange online protection" to "Exchange Online Protection",
        "e o p" to "EOP",
        "s a m l" to "SAML",
        "o auth" to "OAuth",
        "o-auth" to "OAuth",
        "open id" to "OpenID",
        "windows hello for business" to "Windows Hello for Business",
        "w h f b" to "WHfB",
        "company portal" to "Company Portal",
        "end point" to "endpoint",
        "end-point" to "endpoint",
        "v p n" to "VPN",
        "d n s" to "DNS",
        "a p n s" to "APNs",
        "azure active directory" to "Azure Active Directory",
        "microsoft defender" to "Microsoft Defender",
        "microsoft sentinel" to "Microsoft Sentinel",
        "microsoft intune" to "Microsoft Intune",
        "microsoft teams" to "Microsoft Teams",
        "microsoft exchange" to "Microsoft Exchange"
    )

    fun correct(text: String): String {
        var result = text.trim()

        // Apply corrections (case-insensitive)
        corrections.forEach { (wrong, right) ->
            result = result.replace(wrong, right, ignoreCase = true)
        }

        // Fix common speech patterns
        result = fixCommonPatterns(result)

        return result
    }

    private fun fixCommonPatterns(text: String): String {
        var result = text

        // "what is M F A" → "what is MFA"
        result = result.replace(Regex("\\b([A-Z]) ([A-Z]) ([A-Z]) ([A-Z])\\b")) { mr ->
            mr.groupValues.drop(1).joinToString("")
        }
        result = result.replace(Regex("\\b([A-Z]) ([A-Z]) ([A-Z])\\b")) { mr ->
            mr.groupValues.drop(1).joinToString("")
        }
        result = result.replace(Regex("\\b([A-Z]) ([A-Z])\\b")) { mr ->
            mr.groupValues.drop(1).joinToString("")
        }

        return result
    }

    fun correctAndLog(text: String): Pair<String, Boolean> {
        val corrected = correct(text)
        val wasCorrected = corrected != text
        return Pair(corrected, wasCorrected)
    }
}