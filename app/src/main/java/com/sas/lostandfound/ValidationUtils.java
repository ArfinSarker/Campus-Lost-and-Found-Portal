package com.sas.lostandfound;

import android.text.TextUtils;
import android.util.Patterns;

/**
 * Reusable utility class for input validation across the application.
 */
public class ValidationUtils {

    /**
     * Validates if the email follows a standard format.
     */
    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static final String[] COUNTRY_CODES = {
        "+880 (BD)", "+91 (IN)", "+1 (US)", "+44 (GB)", "+966 (SA)", "+971 (AE)",
        "+92 (PK)", "+977 (NP)", "+60 (MY)", "+65 (SG)", "+61 (AU)", "+49 (DE)",
        "+33 (FR)", "+81 (JP)", "+86 (CN)"
    };

    private static java.util.List<Country> countriesList = null;

    public static java.util.List<Country> getCountries() {
        if (countriesList == null) {
            countriesList = new java.util.ArrayList<>();
            countriesList.add(new Country("Afghanistan", "+93", getFlagEmoji("AF"), "AF"));
            countriesList.add(new Country("Aland Islands", "+358", getFlagEmoji("AX"), "AX"));
            countriesList.add(new Country("Albania", "+355", getFlagEmoji("AL"), "AL"));
            countriesList.add(new Country("Algeria", "+213", getFlagEmoji("DZ"), "DZ"));
            countriesList.add(new Country("AmericanSamoa", "+1684", getFlagEmoji("AS"), "AS"));
            countriesList.add(new Country("Andorra", "+376", getFlagEmoji("AD"), "AD"));
            countriesList.add(new Country("Angola", "+244", getFlagEmoji("AO"), "AO"));
            countriesList.add(new Country("Anguilla", "+1264", getFlagEmoji("AI"), "AI"));
            countriesList.add(new Country("Antarctica", "+672", getFlagEmoji("AQ"), "AQ"));
            countriesList.add(new Country("Antigua and Barbuda", "+1268", getFlagEmoji("AG"), "AG"));
            countriesList.add(new Country("Argentina", "+54", getFlagEmoji("AR"), "AR"));
            countriesList.add(new Country("Armenia", "+374", getFlagEmoji("AM"), "AM"));
            countriesList.add(new Country("Aruba", "+297", getFlagEmoji("AW"), "AW"));
            countriesList.add(new Country("Australia", "+61", getFlagEmoji("AU"), "AU"));
            countriesList.add(new Country("Austria", "+43", getFlagEmoji("AT"), "AT"));
            countriesList.add(new Country("Azerbaijan", "+994", getFlagEmoji("AZ"), "AZ"));
            countriesList.add(new Country("Bahamas", "+1242", getFlagEmoji("BS"), "BS"));
            countriesList.add(new Country("Bahrain", "+973", getFlagEmoji("BH"), "BH"));
            countriesList.add(new Country("Bangladesh", "+880", getFlagEmoji("BD"), "BD"));
            countriesList.add(new Country("Barbados", "+1246", getFlagEmoji("BB"), "BB"));
            countriesList.add(new Country("Belarus", "+375", getFlagEmoji("BY"), "BY"));
            countriesList.add(new Country("Belgium", "+32", getFlagEmoji("BE"), "BE"));
            countriesList.add(new Country("Belize", "+501", getFlagEmoji("BZ"), "BZ"));
            countriesList.add(new Country("Benin", "+229", getFlagEmoji("BJ"), "BJ"));
            countriesList.add(new Country("Bermuda", "+1441", getFlagEmoji("BM"), "BM"));
            countriesList.add(new Country("Bhutan", "+975", getFlagEmoji("BT"), "BT"));
            countriesList.add(new Country("Bolivia, Plurinational State of", "+591", getFlagEmoji("BO"), "BO"));
            countriesList.add(new Country("Bonaire, Sint Eustatius and Saba", "+599", getFlagEmoji("BQ"), "BQ"));
            countriesList.add(new Country("Bosnia and Herzegovina", "+387", getFlagEmoji("BA"), "BA"));
            countriesList.add(new Country("Botswana", "+267", getFlagEmoji("BW"), "BW"));
            countriesList.add(new Country("Bouvet Island", "+47", getFlagEmoji("BV"), "BV"));
            countriesList.add(new Country("Brazil", "+55", getFlagEmoji("BR"), "BR"));
            countriesList.add(new Country("British Indian Ocean Territory", "+246", getFlagEmoji("IO"), "IO"));
            countriesList.add(new Country("Brunei Darussalam", "+673", getFlagEmoji("BN"), "BN"));
            countriesList.add(new Country("Bulgaria", "+359", getFlagEmoji("BG"), "BG"));
            countriesList.add(new Country("Burkina Faso", "+226", getFlagEmoji("BF"), "BF"));
            countriesList.add(new Country("Burundi", "+257", getFlagEmoji("BI"), "BI"));
            countriesList.add(new Country("Cambodia", "+855", getFlagEmoji("KH"), "KH"));
            countriesList.add(new Country("Cameroon", "+237", getFlagEmoji("CM"), "CM"));
            countriesList.add(new Country("Canada", "+1", getFlagEmoji("CA"), "CA"));
            countriesList.add(new Country("Cape Verde", "+238", getFlagEmoji("CV"), "CV"));
            countriesList.add(new Country("Cayman Islands", "+1345", getFlagEmoji("KY"), "KY"));
            countriesList.add(new Country("Central African Republic", "+236", getFlagEmoji("CF"), "CF"));
            countriesList.add(new Country("Chad", "+235", getFlagEmoji("TD"), "TD"));
            countriesList.add(new Country("Chile", "+56", getFlagEmoji("CL"), "CL"));
            countriesList.add(new Country("China", "+86", getFlagEmoji("CN"), "CN"));
            countriesList.add(new Country("Christmas Island", "+61", getFlagEmoji("CX"), "CX"));
            countriesList.add(new Country("Cocos (Keeling) Islands", "+61", getFlagEmoji("CC"), "CC"));
            countriesList.add(new Country("Colombia", "+57", getFlagEmoji("CO"), "CO"));
            countriesList.add(new Country("Comoros", "+269", getFlagEmoji("KM"), "KM"));
            countriesList.add(new Country("Congo", "+242", getFlagEmoji("CG"), "CG"));
            countriesList.add(new Country("Congo, The Democratic Republic of the Congo", "+243", getFlagEmoji("CD"), "CD"));
            countriesList.add(new Country("Cook Islands", "+682", getFlagEmoji("CK"), "CK"));
            countriesList.add(new Country("Costa Rica", "+506", getFlagEmoji("CR"), "CR"));
            countriesList.add(new Country("Cote d'Ivoire", "+225", getFlagEmoji("CI"), "CI"));
            countriesList.add(new Country("Croatia", "+385", getFlagEmoji("HR"), "HR"));
            countriesList.add(new Country("Cuba", "+53", getFlagEmoji("CU"), "CU"));
            countriesList.add(new Country("Curaçao", "+599", getFlagEmoji("CW"), "CW"));
            countriesList.add(new Country("Cyprus", "+357", getFlagEmoji("CY"), "CY"));
            countriesList.add(new Country("Czech Republic", "+420", getFlagEmoji("CZ"), "CZ"));
            countriesList.add(new Country("Denmark", "+45", getFlagEmoji("DK"), "DK"));
            countriesList.add(new Country("Djibouti", "+253", getFlagEmoji("DJ"), "DJ"));
            countriesList.add(new Country("Dominica", "+1767", getFlagEmoji("DM"), "DM"));
            countriesList.add(new Country("Dominican Republic", "+1849", getFlagEmoji("DO"), "DO"));
            countriesList.add(new Country("Ecuador", "+593", getFlagEmoji("EC"), "EC"));
            countriesList.add(new Country("Egypt", "+20", getFlagEmoji("EG"), "EG"));
            countriesList.add(new Country("El Salvador", "+503", getFlagEmoji("SV"), "SV"));
            countriesList.add(new Country("Equatorial Guinea", "+240", getFlagEmoji("GQ"), "GQ"));
            countriesList.add(new Country("Eritrea", "+291", getFlagEmoji("ER"), "ER"));
            countriesList.add(new Country("Estonia", "+372", getFlagEmoji("EE"), "EE"));
            countriesList.add(new Country("Ethiopia", "+251", getFlagEmoji("ET"), "ET"));
            countriesList.add(new Country("Falkland Islands (Malvinas)", "+500", getFlagEmoji("FK"), "FK"));
            countriesList.add(new Country("Faroe Islands", "+298", getFlagEmoji("FO"), "FO"));
            countriesList.add(new Country("Fiji", "+679", getFlagEmoji("FJ"), "FJ"));
            countriesList.add(new Country("Finland", "+358", getFlagEmoji("FI"), "FI"));
            countriesList.add(new Country("France", "+33", getFlagEmoji("FR"), "FR"));
            countriesList.add(new Country("French Guiana", "+594", getFlagEmoji("GF"), "GF"));
            countriesList.add(new Country("French Polynesia", "+689", getFlagEmoji("PF"), "PF"));
            countriesList.add(new Country("French Southern Territories", "+262", getFlagEmoji("TF"), "TF"));
            countriesList.add(new Country("Gabon", "+241", getFlagEmoji("GA"), "GA"));
            countriesList.add(new Country("Gambia", "+220", getFlagEmoji("GM"), "GM"));
            countriesList.add(new Country("Georgia", "+995", getFlagEmoji("GE"), "GE"));
            countriesList.add(new Country("Germany", "+49", getFlagEmoji("DE"), "DE"));
            countriesList.add(new Country("Ghana", "+233", getFlagEmoji("GH"), "GH"));
            countriesList.add(new Country("Gibraltar", "+350", getFlagEmoji("GI"), "GI"));
            countriesList.add(new Country("Greece", "+30", getFlagEmoji("GR"), "GR"));
            countriesList.add(new Country("Greenland", "+299", getFlagEmoji("GL"), "GL"));
            countriesList.add(new Country("Grenada", "+1473", getFlagEmoji("GD"), "GD"));
            countriesList.add(new Country("Guadeloupe", "+590", getFlagEmoji("GP"), "GP"));
            countriesList.add(new Country("Guam", "+1671", getFlagEmoji("GU"), "GU"));
            countriesList.add(new Country("Guatemala", "+502", getFlagEmoji("GT"), "GT"));
            countriesList.add(new Country("Guernsey", "+44", getFlagEmoji("GG"), "GG"));
            countriesList.add(new Country("Guinea", "+224", getFlagEmoji("GN"), "GN"));
            countriesList.add(new Country("Guinea-Bissau", "+245", getFlagEmoji("GW"), "GW"));
            countriesList.add(new Country("Guyana", "+595", getFlagEmoji("GY"), "GY"));
            countriesList.add(new Country("Haiti", "+509", getFlagEmoji("HT"), "HT"));
            countriesList.add(new Country("Heard Island and McDonald Islands", "+672", getFlagEmoji("HM"), "HM"));
            countriesList.add(new Country("Holy See (Vatican City State)", "+379", getFlagEmoji("VA"), "VA"));
            countriesList.add(new Country("Honduras", "+504", getFlagEmoji("HN"), "HN"));
            countriesList.add(new Country("Hong Kong", "+852", getFlagEmoji("HK"), "HK"));
            countriesList.add(new Country("Hungary", "+36", getFlagEmoji("HU"), "HU"));
            countriesList.add(new Country("Iceland", "+354", getFlagEmoji("IS"), "IS"));
            countriesList.add(new Country("India", "+91", getFlagEmoji("IN"), "IN"));
            countriesList.add(new Country("Indonesia", "+62", getFlagEmoji("ID"), "ID"));
            countriesList.add(new Country("Iran, Islamic Republic of Persian Gulf", "+98", getFlagEmoji("IR"), "IR"));
            countriesList.add(new Country("Iraq", "+964", getFlagEmoji("IQ"), "IQ"));
            countriesList.add(new Country("Ireland", "+353", getFlagEmoji("IE"), "IE"));
            countriesList.add(new Country("Isle of Man", "+44", getFlagEmoji("IM"), "IM"));
            countriesList.add(new Country("Israel", "+972", getFlagEmoji("IL"), "IL"));
            countriesList.add(new Country("Italy", "+39", getFlagEmoji("IT"), "IT"));
            countriesList.add(new Country("Jamaica", "+1876", getFlagEmoji("JM"), "JM"));
            countriesList.add(new Country("Japan", "+81", getFlagEmoji("JP"), "JP"));
            countriesList.add(new Country("Jersey", "+44", getFlagEmoji("JE"), "JE"));
            countriesList.add(new Country("Jordan", "+962", getFlagEmoji("JO"), "JO"));
            countriesList.add(new Country("Kazakhstan", "+77", getFlagEmoji("KZ"), "KZ"));
            countriesList.add(new Country("Kenya", "+254", getFlagEmoji("KE"), "KE"));
            countriesList.add(new Country("Kiribati", "+686", getFlagEmoji("KI"), "KI"));
            countriesList.add(new Country("Korea, Democratic People's Republic of Korea", "+850", getFlagEmoji("KP"), "KP"));
            countriesList.add(new Country("Korea, Republic of South Korea", "+82", getFlagEmoji("KR"), "KR"));
            countriesList.add(new Country("Kosovo", "+383", getFlagEmoji("XK"), "XK"));
            countriesList.add(new Country("Kuwait", "+965", getFlagEmoji("KW"), "KW"));
            countriesList.add(new Country("Kyrgyzstan", "+996", getFlagEmoji("KG"), "KG"));
            countriesList.add(new Country("Laos", "+856", getFlagEmoji("LA"), "LA"));
            countriesList.add(new Country("Latvia", "+371", getFlagEmoji("LV"), "LV"));
            countriesList.add(new Country("Lebanon", "+961", getFlagEmoji("LB"), "LB"));
            countriesList.add(new Country("Lesotho", "+266", getFlagEmoji("LS"), "LS"));
            countriesList.add(new Country("Liberia", "+231", getFlagEmoji("LR"), "LR"));
            countriesList.add(new Country("Libyan Arab Jamahiriya", "+218", getFlagEmoji("LY"), "LY"));
            countriesList.add(new Country("Liechtenstein", "+423", getFlagEmoji("LI"), "LI"));
            countriesList.add(new Country("Lithuania", "+370", getFlagEmoji("LT"), "LT"));
            countriesList.add(new Country("Luxembourg", "+352", getFlagEmoji("LU"), "LU"));
            countriesList.add(new Country("Macao", "+853", getFlagEmoji("MO"), "MO"));
            countriesList.add(new Country("Macedonia", "+389", getFlagEmoji("MK"), "MK"));
            countriesList.add(new Country("Madagascar", "+261", getFlagEmoji("MG"), "MG"));
            countriesList.add(new Country("Malawi", "+265", getFlagEmoji("MW"), "MW"));
            countriesList.add(new Country("Malaysia", "+60", getFlagEmoji("MY"), "MY"));
            countriesList.add(new Country("Maldives", "+960", getFlagEmoji("MV"), "MV"));
            countriesList.add(new Country("Mali", "+223", getFlagEmoji("ML"), "ML"));
            countriesList.add(new Country("Malta", "+356", getFlagEmoji("MT"), "MT"));
            countriesList.add(new Country("Marshall Islands", "+692", getFlagEmoji("MH"), "MH"));
            countriesList.add(new Country("Martinique", "+596", getFlagEmoji("MQ"), "MQ"));
            countriesList.add(new Country("Mauritania", "+222", getFlagEmoji("MR"), "MR"));
            countriesList.add(new Country("Mauritius", "+230", getFlagEmoji("MU"), "MU"));
            countriesList.add(new Country("Mayotte", "+262", getFlagEmoji("YT"), "YT"));
            countriesList.add(new Country("Mexico", "+52", getFlagEmoji("MX"), "MX"));
            countriesList.add(new Country("Micronesia, Federated States of Micronesia", "+691", getFlagEmoji("FM"), "FM"));
            countriesList.add(new Country("Moldova", "+373", getFlagEmoji("MD"), "MD"));
            countriesList.add(new Country("Monaco", "+377", getFlagEmoji("MC"), "MC"));
            countriesList.add(new Country("Mongolia", "+976", getFlagEmoji("MN"), "MN"));
            countriesList.add(new Country("Montenegro", "+382", getFlagEmoji("ME"), "ME"));
            countriesList.add(new Country("Montserrat", "+1664", getFlagEmoji("MS"), "MS"));
            countriesList.add(new Country("Morocco", "+212", getFlagEmoji("MA"), "MA"));
            countriesList.add(new Country("Mozambique", "+258", getFlagEmoji("MZ"), "MZ"));
            countriesList.add(new Country("Myanmar", "+95", getFlagEmoji("MM"), "MM"));
            countriesList.add(new Country("Namibia", "+264", getFlagEmoji("NA"), "NA"));
            countriesList.add(new Country("Nauru", "+674", getFlagEmoji("NR"), "NR"));
            countriesList.add(new Country("Nepal", "+977", getFlagEmoji("NP"), "NP"));
            countriesList.add(new Country("Netherlands", "+31", getFlagEmoji("NL"), "NL"));
            countriesList.add(new Country("Netherlands Antilles", "+599", getFlagEmoji("AN"), "AN"));
            countriesList.add(new Country("New Caledonia", "+687", getFlagEmoji("NC"), "NC"));
            countriesList.add(new Country("New Zealand", "+64", getFlagEmoji("NZ"), "NZ"));
            countriesList.add(new Country("Nicaragua", "+505", getFlagEmoji("NI"), "NI"));
            countriesList.add(new Country("Niger", "+227", getFlagEmoji("NE"), "NE"));
            countriesList.add(new Country("Nigeria", "+234", getFlagEmoji("NG"), "NG"));
            countriesList.add(new Country("Niue", "+683", getFlagEmoji("NU"), "NU"));
            countriesList.add(new Country("Norfolk Island", "+672", getFlagEmoji("NF"), "NF"));
            countriesList.add(new Country("Northern Mariana Islands", "+1670", getFlagEmoji("MP"), "MP"));
            countriesList.add(new Country("Norway", "+47", getFlagEmoji("NO"), "NO"));
            countriesList.add(new Country("Oman", "+968", getFlagEmoji("OM"), "OM"));
            countriesList.add(new Country("Pakistan", "+92", getFlagEmoji("PK"), "PK"));
            countriesList.add(new Country("Palau", "+680", getFlagEmoji("PW"), "PW"));
            countriesList.add(new Country("Palestinian Territory, Occupied", "+970", getFlagEmoji("PS"), "PS"));
            countriesList.add(new Country("Panama", "+507", getFlagEmoji("PA"), "PA"));
            countriesList.add(new Country("Papua New Guinea", "+675", getFlagEmoji("PG"), "PG"));
            countriesList.add(new Country("Paraguay", "+595", getFlagEmoji("PY"), "PY"));
            countriesList.add(new Country("Peru", "+51", getFlagEmoji("PE"), "PE"));
            countriesList.add(new Country("Philippines", "+63", getFlagEmoji("PH"), "PH"));
            countriesList.add(new Country("Pitcairn", "+872", getFlagEmoji("PN"), "PN"));
            countriesList.add(new Country("Poland", "+48", getFlagEmoji("PL"), "PL"));
            countriesList.add(new Country("Portugal", "+351", getFlagEmoji("PT"), "PT"));
            countriesList.add(new Country("Puerto Rico", "+1939", getFlagEmoji("PR"), "PR"));
            countriesList.add(new Country("Qatar", "+974", getFlagEmoji("QA"), "QA"));
            countriesList.add(new Country("Romania", "+40", getFlagEmoji("RO"), "RO"));
            countriesList.add(new Country("Russia", "+7", getFlagEmoji("RU"), "RU"));
            countriesList.add(new Country("Rwanda", "+250", getFlagEmoji("RW"), "RW"));
            countriesList.add(new Country("Reunion", "+262", getFlagEmoji("RE"), "RE"));
            countriesList.add(new Country("Saint Barthelemy", "+590", getFlagEmoji("BL"), "BL"));
            countriesList.add(new Country("Saint Helena, Ascension and Tristan Da Cunha", "+290", getFlagEmoji("SH"), "SH"));
            countriesList.add(new Country("Saint Kitts and Nevis", "+1869", getFlagEmoji("KN"), "KN"));
            countriesList.add(new Country("Saint Lucia", "+1758", getFlagEmoji("LC"), "LC"));
            countriesList.add(new Country("Saint Martin", "+590", getFlagEmoji("MF"), "MF"));
            countriesList.add(new Country("Saint Pierre and Miquelon", "+508", getFlagEmoji("PM"), "PM"));
            countriesList.add(new Country("Saint Vincent and the Grenadines", "+1784", getFlagEmoji("VC"), "VC"));
            countriesList.add(new Country("Samoa", "+685", getFlagEmoji("WS"), "WS"));
            countriesList.add(new Country("San Marino", "+378", getFlagEmoji("SM"), "SM"));
            countriesList.add(new Country("Sao Tome and Principe", "+239", getFlagEmoji("ST"), "ST"));
            countriesList.add(new Country("Saudi Arabia", "+966", getFlagEmoji("SA"), "SA"));
            countriesList.add(new Country("Senegal", "+221", getFlagEmoji("SN"), "SN"));
            countriesList.add(new Country("Serbia", "+381", getFlagEmoji("RS"), "RS"));
            countriesList.add(new Country("Seychelles", "+248", getFlagEmoji("SC"), "SC"));
            countriesList.add(new Country("Sierra Leone", "+232", getFlagEmoji("SL"), "SL"));
            countriesList.add(new Country("Singapore", "+65", getFlagEmoji("SG"), "SG"));
            countriesList.add(new Country("Sint Maarten", "+1721", getFlagEmoji("SX"), "SX"));
            countriesList.add(new Country("Slovakia", "+421", getFlagEmoji("SK"), "SK"));
            countriesList.add(new Country("Slovenia", "+386", getFlagEmoji("SI"), "SI"));
            countriesList.add(new Country("Solomon Islands", "+677", getFlagEmoji("SB"), "SB"));
            countriesList.add(new Country("Somalia", "+252", getFlagEmoji("SO"), "SO"));
            countriesList.add(new Country("South Africa", "+27", getFlagEmoji("ZA"), "ZA"));
            countriesList.add(new Country("South Sudan", "+211", getFlagEmoji("SS"), "SS"));
            countriesList.add(new Country("South Georgia and the South Sandwich Islands", "+500", getFlagEmoji("GS"), "GS"));
            countriesList.add(new Country("Spain", "+34", getFlagEmoji("ES"), "ES"));
            countriesList.add(new Country("Sri Lanka", "+94", getFlagEmoji("LK"), "LK"));
            countriesList.add(new Country("Sudan", "+249", getFlagEmoji("SD"), "SD"));
            countriesList.add(new Country("Suriname", "+597", getFlagEmoji("SR"), "SR"));
            countriesList.add(new Country("Svalbard and Jan Mayen", "+47", getFlagEmoji("SJ"), "SJ"));
            countriesList.add(new Country("Eswatini (Swaziland)", "+268", getFlagEmoji("SZ"), "SZ"));
            countriesList.add(new Country("Sweden", "+46", getFlagEmoji("SE"), "SE"));
            countriesList.add(new Country("Switzerland", "+41", getFlagEmoji("CH"), "CH"));
            countriesList.add(new Country("Syrian Arab Republic", "+963", getFlagEmoji("SY"), "SY"));
            countriesList.add(new Country("Taiwan", "+886", getFlagEmoji("TW"), "TW"));
            countriesList.add(new Country("Tajikistan", "+992", getFlagEmoji("TJ"), "TJ"));
            countriesList.add(new Country("Tanzania, United Republic of Tanzania", "+255", getFlagEmoji("TZ"), "TZ"));
            countriesList.add(new Country("Thailand", "+66", getFlagEmoji("TH"), "TH"));
            countriesList.add(new Country("Timor-Leste", "+670", getFlagEmoji("TL"), "TL"));
            countriesList.add(new Country("Togo", "+228", getFlagEmoji("TG"), "TG"));
            countriesList.add(new Country("Tokelau", "+690", getFlagEmoji("TK"), "TK"));
            countriesList.add(new Country("Tonga", "+676", getFlagEmoji("TO"), "TO"));
            countriesList.add(new Country("Trinidad and Tobago", "+1868", getFlagEmoji("TT"), "TT"));
            countriesList.add(new Country("Tunisia", "+216", getFlagEmoji("TN"), "TN"));
            countriesList.add(new Country("Turkey", "+90", getFlagEmoji("TR"), "TR"));
            countriesList.add(new Country("Turkmenistan", "+993", getFlagEmoji("TM"), "TM"));
            countriesList.add(new Country("Turks and Caicos Islands", "+1649", getFlagEmoji("TC"), "TC"));
            countriesList.add(new Country("Tuvalu", "+688", getFlagEmoji("TV"), "TV"));
            countriesList.add(new Country("Uganda", "+256", getFlagEmoji("UG"), "UG"));
            countriesList.add(new Country("Ukraine", "+380", getFlagEmoji("UA"), "UA"));
            countriesList.add(new Country("United Arab Emirates", "+971", getFlagEmoji("AE"), "AE"));
            countriesList.add(new Country("United Kingdom", "+44", getFlagEmoji("GB"), "GB"));
            countriesList.add(new Country("United States Minor Outlying Islands", "+1", getFlagEmoji("UM"), "UM"));
            countriesList.add(new Country("United States", "+1", getFlagEmoji("US"), "US"));
            countriesList.add(new Country("Uruguay", "+598", getFlagEmoji("UY"), "UY"));
            countriesList.add(new Country("Uzbekistan", "+998", getFlagEmoji("UZ"), "UZ"));
            countriesList.add(new Country("Vanuatu", "+678", getFlagEmoji("VU"), "VU"));
            countriesList.add(new Country("Venezuela, Bolivarian Republic of Venezuela", "+58", getFlagEmoji("VE"), "VE"));
            countriesList.add(new Country("Vietnam", "+84", getFlagEmoji("VN"), "VN"));
            countriesList.add(new Country("Virgin Islands, British", "+1284", getFlagEmoji("VG"), "VG"));
            countriesList.add(new Country("Virgin Islands, U.S.", "+1340", getFlagEmoji("VI"), "VI"));
            countriesList.add(new Country("Wallis and Futuna", "+681", getFlagEmoji("WF"), "WF"));
            countriesList.add(new Country("Western Sahara", "+212", getFlagEmoji("EH"), "EH"));
            countriesList.add(new Country("Yemen", "+967", getFlagEmoji("YE"), "YE"));
            countriesList.add(new Country("Zambia", "+260", getFlagEmoji("ZM"), "ZM"));
            countriesList.add(new Country("Zimbabwe", "+263", getFlagEmoji("ZW"), "ZW"));
        }
        return countriesList;
    }

    public static String getFlagEmoji(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) {
            return "";
        }
        int firstLetter = Character.codePointAt(countryCode.toUpperCase(), 0) - 'A' + 0x1F1E6;
        int secondLetter = Character.codePointAt(countryCode.toUpperCase(), 1) - 'A' + 0x1F1E6;
        return new String(Character.toChars(firstLetter)) + new String(Character.toChars(secondLetter));
    }

    public static String getCountryDisplayString(String rawCode) {
        if (TextUtils.isEmpty(rawCode)) {
            return "🇧🇩 +880";
        }
        String cleanCode = rawCode.trim();
        if (!cleanCode.startsWith("+") && !cleanCode.startsWith("0")) {
            cleanCode = "+" + cleanCode;
        }
        // Resolve ambiguities for shared dial codes by returning the most prominent country
        if ("+1".equals(cleanCode)) return "🇺🇸 +1";
        if ("+44".equals(cleanCode)) return "🇬🇧 +44";
        if ("+7".equals(cleanCode)) return "🇷🇺 +7";
        if ("+358".equals(cleanCode)) return "🇫🇮 +358";
        if ("+599".equals(cleanCode)) return "🇨🇼 +599";
        if ("+590".equals(cleanCode)) return "🇬🇵 +590";
        
        for (Country country : getCountries()) {
            if (country.getCode().equals(cleanCode)) {
                return country.getFlagEmoji() + " " + country.getCode();
            }
        }
        return "🇧🇩 " + cleanCode;
    }

    public static String extractCountryCode(String displayText) {
        if (TextUtils.isEmpty(displayText)) {
            return "+880";
        }
        String cleanText = displayText.trim();
        String[] parts = cleanText.split("\\s+");
        if (parts.length >= 2) {
            return parts[1];
        }
        return parts[0];
    }

    private static java.util.List<String> sortedDialCodes = null;

    private static synchronized void ensureSortedDialCodes() {
        if (sortedDialCodes == null) {
            sortedDialCodes = new java.util.ArrayList<>();
            for (Country c : getCountries()) {
                String code = c.getCode();
                if (!sortedDialCodes.contains(code)) {
                    sortedDialCodes.add(code);
                }
            }
            // Sort dial codes by length descending so longest matches first
            java.util.Collections.sort(sortedDialCodes, (s1, s2) -> Integer.compare(s2.length(), s1.length()));
        }
    }

    /**
     * Parses a single full phone number into a country code and phone body.
     */
    public static String[] parsePhoneNumber(String fullPhone) {
        if (TextUtils.isEmpty(fullPhone)) {
            return new String[]{"+880", ""};
        }
        ensureSortedDialCodes();
        for (String prefix : sortedDialCodes) {
            if (fullPhone.startsWith(prefix)) {
                return new String[]{prefix, fullPhone.substring(prefix.length())};
            }
        }
        // Fallback for raw legacy Bangladeshi mobile numbers without code (e.g. 017...)
        if (fullPhone.startsWith("0") && fullPhone.length() == 11) {
            return new String[]{"+880", fullPhone.substring(1)}; // Strip leading 0
        }
        if (fullPhone.startsWith("1") && fullPhone.length() == 10) {
            return new String[]{"+880", fullPhone};
        }
        return new String[]{"+880", fullPhone};
    }

    /**
     * Validates a phone number based on country code and body.
     */
    public static boolean isValidPhone(String countryCode, String phoneBody) {
        if (TextUtils.isEmpty(countryCode) || TextUtils.isEmpty(phoneBody)) return false;
        
        // Strip any spaces, dashes, or parentheses
        String cleanBody = phoneBody.replaceAll("[\\s\\-\\(\\)]", "");
        
        if ("+880".equals(countryCode)) {
            // Bangladesh mobile number: body should be 10 digits starting with 1, 
            // or 11 digits starting with 01
            return cleanBody.matches("01[3-9]\\d{8}") || cleanBody.matches("[3-9]\\d{8}") || cleanBody.matches("1[3-9]\\d{8}");
        }
        
        // General international validation: 6 to 14 digits
        return cleanBody.matches("\\d{6,14}");
    }

    /**
     * Backward-compatible validation wrapper for full phone numbers.
     */
    public static boolean isValidPhone(String fullPhone) {
        if (TextUtils.isEmpty(fullPhone)) return false;
        String[] parsed = parsePhoneNumber(fullPhone);
        return isValidPhone(parsed[0], parsed[1]);
    }

    /**
     * Validates if the password is at least 8 characters long and contains at least 
     * one uppercase letter, one lowercase letter, and one digit.
     */
    public static boolean isValidPassword(String password) {
        if (TextUtils.isEmpty(password) || password.length() < 8) return false;
        
        boolean hasUppercase = !password.equals(password.toLowerCase());
        boolean hasLowercase = !password.equals(password.toUpperCase());
        boolean hasDigit = password.matches(".*\\d.*");
        
        return hasUppercase && hasLowercase && hasDigit;
    }
    
    /**
     * Returns the requirements for a valid password.
     */
    public static String getPasswordRequirements() {
        return "Password must be at least 8 characters and include uppercase, lowercase, and a number.";
    }
}
