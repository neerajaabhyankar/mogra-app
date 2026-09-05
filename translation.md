# Translation — mogra 0.1

Every user-visible string in the app, and the source of truth for all three languages.

**This file generates the resources.** Edit a cell, then run:

```
python3 tools/gen_strings.py
```

which rewrites `res/values/`, `res/values-mr/` and `res/values-hi/`. Never edit those XML
files by hand — the next run overwrites them. The generator refuses to write if a locale's
format specifiers do not match English exactly, and a unit test refuses to pass if a string
reaches the app without appearing here.

Anything added to the app later lands here too, with `TODO` in the note column.

**Not translated, deliberately:** `mogra` (see question 1), `Hz`, note names like `C♯3`,
version numbers, and the language chips themselves — `English / मराठी / हिंदी` stay in their
own language in every build, which is the convention everywhere.

**Register.** I have used the ordinary polite imperative throughout — Marathi *गा*, Hindi
*गाइए*. If you would rather the app spoke more plainly (*गाओ*) or more formally, say so once
and I will sweep the whole table.

---

## 1. Home

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `app_name` | mogra | मोगरा | मोगरा | the wordmark — see question 1 |
| `app_subtitle` | Tools for Hindustani Classical Music | हिंदुस्तानी शास्त्रीय संगीतासाठी साधने | हिंदुस्तानी शास्त्रीय संगीत के लिए साधन | |
| `section_tools` | Tools | साधने | साधन | small caps in the UI |
| `badge_soon` | SOON | लवकरच | जल्द ही | |
| `tool_identifier_title` | Raag Identifier | राग ओळखू | राग पहचानें | |
| `tool_identifier_blurb` | Hum, sing, or play. Get the five most likely raags. | गुणगुणा, गा, किंवा वाजवा. सर्वात जवळचे पाच राग मिळवा. | गुनगुनाइए, गाइए या बजाइए. सबसे संभावित पाँच राग पाइए. | |
| `tool_by_notes_title` | Raagfinder by Notes | स्वरांवरून राग शोधू | स्वरों से राग खोजें | |
| `tool_by_notes_blurb` | Pick the swars you heard. See which raags fit. | ऐकलेले स्वर निवडा. कोणते राग जुळतात ते पाहा. | सुने हुए स्वर चुनिए. देखिए कौन से राग बैठते हैं. | |
| `tool_by_name_title` | Raag Lookup | राग माहिती | राग जानकारी | |
| `tool_by_name_blurb` | Look up a raag: thaat, aaroha, avaroha, chalan. | राग पाहा: थाट, आरोह, अवरोह, चलन. | राग देखिए: थाट, आरोह, अवरोह, चलन. | |

## 2. Set Sa

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `sa_title` | Where is your Sa? | तुमचा 'सा' कोणता? | आपका 'सा' कौन सा? | the big serif line |
| `sa_subtitle` | Needed for better accuracy! | अधिक अचूकतेसाठी गरजेचं! | बेहतर सटीकता के लिए ज़रूरी! | |
| `sa_tab_hum` | Hum it | गुणगुणा | गुनगुनाइए | must fit a third of the width |
| `sa_tab_note` | Pick a note | स्वर निवडा | स्वर चुनिए | same |
| `sa_tab_hz` | Enter Hz | Hz सांगा | Hz बताइये | same |
| `sa_keyboard_hint` | Drag the keys sideways | पट्टी आडवी सरकवा | पट्टी दोनों तरफ खिसकाइए | sits between the A2 and E4 labels |
| `sa_hum_idle` | Hum Sa | 'सा' द्या | 'सा' शुरू | inside the circle, keep it short |
| `sa_hum_holding` | Hold one steady note. | एकच स्वर स्थिर धरा. | एक ही स्वर स्थिर रखिए. | while listening |
| `sa_hum_prompt` | Tap, then hold Sa for about five seconds. | टॅप करा, मग सुमारे पाच सेकंद सा धरा. | टैप कीजिए, फिर लगभग पाँच सेकंड सा रखिए. | before listening |
| `sa_hz_hint` | Nudge to the exact frequency. | नेमकी कंपनसंख्या जुळवा. | सटीक कंपन आवृत्ति मिलाइए. | *कंपनसंख्या / आवृत्ति* are correct |
| `sa_label` | Sa | सा | सा | the small caps label on the card |
| `sa_play` | Play Sa | सा ऐका | सा सुनिए | |
| `sa_next` | Next: record | पुढे: रेकॉर्ड | आगे: रेकॉर्ड | the red button |
| `sa_remembered` | Your Sa is remembered for next time. | तुमचा सा पुढच्या वेळेसाठी लक्षात ठेवला जाईल. | आपका सा अगली बार के लिए याद रखा जाएगा. | |
| `peg_black` | Kali %s | काळी %s | काली %s | placeholders are %s, not %d: the app formats the digits itself so Marathi gets Devanagari |
| `peg_white` | Safed %s | पांढरी %s | सफ़ेद %s | placeholders are %s, not %d: the app formats the digits itself so Marathi gets Devanagari |
| `cents_sharp` | +%s cents | +%s शतांश | +%s शतांश | placeholders are %s, not %d: the app formats the digits itself so Marathi gets Devanagari |
| `cents_flat` | −%s cents | −%s शतांश | −%s शतांश | placeholders are %s, not %d: the app formats the digits itself so Marathi gets Devanagari |

## 3. Record

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `rec_change_sa` | Change | बदला | बदलिए | on the Sa strip, tight space |
| `rec_title` | Sing, hum or play | गुणगुणा किंवा वाजवा | गुनगुनाइए या बजाइए | |
| `rec_blurb` | One voice or instrument, close to the phone. A tanpura or tabla in the background is fine. | एकच आवाज किंवा वाद्य, फोनजवळ. मागे तानपुरा किंवा तबला चालेल. | एक ही आवाज़ या वाद्य, फ़ोन के पास. पीछे तानपुरा या तबला चले तो ठीक है. | |
| `rec_start` | Record | रेकॉर्ड | रेकॉर्ड | inside the circle |
| `rec_again` | Again | पुन्हा | फिर से | inside the circle, once there is a take |
| `rec_keep_going_min` | Keep going for at least 20 seconds | किमान २० सेकंद चालू ठेवा | कम से कम 20 सेकंड जारी रखिए | |
| `rec_keep_going_more` | Keep going, the longer the better! | चालू ठेवा, जितका वेळ तितकं बरं! | जारी रखिए, जितनी देर उतना अच्छा! | |
| `rec_stay_open` | Stay on this screen — leaving the app stops the recording. | या स्क्रीनवर थांबा — अ‍ॅप सोडल्यास रेकॉर्डिंग थांबेल. | इसी स्क्रीन पर रहिए — ऐप छोड़ने पर रेकॉर्डिंग रुक जाएगी. | only while recording |
| `rec_identify` | Identify the raag | राग ओळखा | राग पहचानिए | the red button |

## 4. Analysing

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `analysing_title` | Analyzing | विश्लेषण चालू | विश्लेषण जारी | English is one word; both of these are three. Shorter options: *तपासत आहे* / *जाँच जारी* |
| `analysing_private` | Nothing is uploaded — this runs on your phone. | काहीही अपलोड होत नाही — हे तुमच्या फोनवरच चालतं. | कुछ भी अपलोड नहीं होता — यह आपके फ़ोन पर ही चलता है. | |

## 5. Result

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `result_title` | Result | निष्कर्ष | निष्कर्ष | header, small caps |
| `result_most_likely` | Most likely | सर्वात जवळचे | सबसे संभावित | |
| `result_trust_title` | How much to trust this | यावर किती विश्वास ठेवावा | इस पर कितना भरोसा करें | |
| `result_trust_body` | The algorithm knows only 50 raags and will still guess if yours is not one of them. On professional recordings, the top guess is right about half the time, and the true raag is somewhere in these five about four times in five. Expect worse with casual humming. | ह्या अ‍ॅल्गॉरिदमला फक्त ५० राग माहीत आहेत; तुमचा राग त्यांत नसला तरी ते आपले अंदाज देतंच. व्यावसायिक रेकॉर्डिंगवर पहिला अंदाज साधारण निम्म्या वेळा बरोबर असतो, आणि खरा राग या पाचांत ८० टक्के वेळा असतो. सहज गुणगुणल्यावर याहून अधिक चुका अपेक्षित आहेत. | यह अल्गोरिदम सिर्फ़ 50 राग जानती है और आपका राग उनमें न हो तब भी अंदाज़ा देती ही है. पेशेवर रेकॉर्डिंग पर पहला अंदाज़ा लगभग आधी बार सही होता है, और असली राग इन पाँच में 80 प्रतिशत बार होता है. यूँ ही गुनगुनाने पर इससे कम उम्मीद रखिए. | your English wording, carried over |
| `result_record_again` | Record again | पुन्हा रेकॉर्ड करा | फिर से रेकॉर्ड करें | |
| `result_change_sa` | Change Sa | सा बदला | सा बदलिए | |
| `result_footer` | Sa %1$s · %2$s s · %3$s windows · 50 raags | सा %1$s · %2$s से · %3$s खंड · ५० राग | सा %1$s · %2$s से · %3$s खंड · 50 राग | placeholders are %s, not %d: the app formats the digits itself so Marathi gets Devanagari |

## 6. Placeholder screen

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `wip_title` | Not built yet | अजून तयार नाही | अभी तैय्यार नहीं | |
| `wip_body` | This screen is soon to be built. | ही स्क्रीन लवकरच तयार होणार आहे. | यह स्क्रीन जल्द ही बनेगी. | |

## 7. Errors

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `err_no_pitch` | Could not hear a steady pitch — hum one note, louder, for about 5 seconds. | स्थिर स्वर ऐकू आला नाही — एकच स्वर, थोडं मोठ्यानं, सुमारे ५ सेकंद गुणगुणा. | स्थिर स्वर सुनाई नहीं दिया — एक ही स्वर, थोड़ा ज़ोर से, लगभग 5 सेकंड गुनगुनाइए. | |
| `err_no_mic` | Microphone unavailable | मायक्रोफोन उपलब्ध नाही | माइक्रोफ़ोन उपलब्ध नहीं | |
| `err_record_failed` | Recording failed | रेकॉर्डिंग होऊ शकलं नाही | रेकॉर्डिंग नहीं हो पाई | |
| `err_identify_failed` | Could not identify — try again | ओळखता आलं नाही — पुन्हा प्रयत्न करा | पहचान नहीं हो पाई — फिर कोशिश कीजिए | currently shows a raw exception name |

## 8. Spoken only (screen readers — never displayed)

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `cd_back` | Back | मागे | पीछे | |
| `cd_record_start` | Start recording | रेकॉर्डिंग सुरू | रेकॉर्डिंग शुरू | |
| `cd_record_stop` | Stop recording | रेकॉर्डिंग बंद | रेकॉर्डिंग खतम | |
| `cd_record_again` | Record again | पुन्हा रेकॉर्ड करा | फिर से रेकॉर्ड कीजिए | |
| `cd_hum_sa` | Hum your Sa | तुमचा सा गुणगुणा | अपना सा गुनगुनाइए | |
| `cd_play_sa` | Play Sa | सा ऐका | सा सुनिए | |
| `cd_change_sa` | Change Sa | सा बदला | सा बदलिए | |
| `cd_coming_soon` | Coming soon. | लवकरच. | जल्द ही. | appended to the two disabled cards |
| `cd_result_row` | %1$s, %2$s percent | %1$s, %2$s टक्के | %1$s, %2$s प्रतिशत | placeholders are %s, not %d: the app formats the digits itself so Marathi gets Devanagari |

## 8b. Added since the first review

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `percent_only` | %s%% | %s%% | %s%% | placeholders are %s, not %d: the app formats the digits itself so Marathi gets Devanagari |
| `step_of` | %1$s / %2$s | %1$s / %2$s | %1$s / %2$s | placeholders are %s, not %d: the app formats the digits itself so Marathi gets Devanagari |

## 10. Raagfinder by Notes

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `notes_title` | Which swars does the Raag have? | रागात कोणते स्वर आहेत? | राग में कौन से स्वर हैं?| |
| `notes_subtitle` | Tap the swars. Raags using exactly that set will show up. | स्वर निवडा. नेमके तेच स्वर वापरणारे राग सुचवले जातील. | स्वर चुनिए. ठीक वही स्वर इस्तेमाल करने वाले राग दिखाए जाएंगे. | |
| `notes_clear` | Clear | खोडा | मिटाइए | |
| `notes_prompt` | Pick at least one swar. | किमान एक स्वर निवडा. | कम से कम एक स्वर चुनिए. | |
| `notes_none` | No raag in the database uses exactly these swars. | नेमके हेच स्वर वापरणारा राग डेटाबेसमध्ये नाही. | ठीक यही स्वर इस्तेमाल करने वाला राग डेटाबेस में नहीं है. | |
| `notes_matches` | Exact matches | नेमके जुळणारे | पूरी तरह मिलते हुए | |

## 11. Raagfinder by Name

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `name_title` | Look up a raag | राग माहिती पाहा | राग जनकारी देखिए | |
| `name_pick` | Choose a raag | राग निवडा | राग चुनिए | |
| `name_filter` | Filter by name | नावाने शोधा | नाम से खोजिए | |

## 12. Raag attributes

The table both Raagfinder tools show. The values themselves — swars, thaat names, prahar —
come from the tanarang database and are not translated.

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `attr_aaroha` | Aaroha | आरोह | आरोह | |
| `attr_avaroha` | Avaroha | अवरोह | अवरोह | |
| `attr_mukhyanga` | Mukhyanga | मुख्यांग | मुख्यांग | |
| `attr_aarohi_nyas` | Aarohi nyas | आरोही न्यास | आरोही न्यास | |
| `attr_avarohi_nyas` | Avarohi nyas | अवरोही न्यास | अवरोही न्यास | |
| `attr_vaadi` | Vaadi | वादी | वादी | |
| `attr_samvaadi` | Samvaadi | संवादी | संवादी | |
| `attr_thaat` | Thaat | थाट | थाट | |
| `attr_prahar` | Prahar | प्रहर | प्रहर | |
| `credit_tanarang` | Raag data from tanarang.com | राग माहिती tanarang.com वरून | राग जानकारी tanarang.com से | |
| `cd_swar` | Swar %s | स्वर %s | स्वर %s | — spoken only |


## 18. Sa drift warning

Shown on the Result page when the recording's own pitch histogram has a peak 10-50 cents
from the Sa that was given. Closer than 10 and nobody could act on it; further than 50 and
the peak is a different swar rather than a drifted Sa.

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `sa_drift_above` | The recording sits %s cents above the Sa you gave. | तुम्ही दिलेल्या साच्या %s शतांश वर रेकॉर्डिंग आहे. | आपने दिए सा से %s शतांश ऊपर रेकॉर्डिंग है. | |
| `sa_drift_below` | The recording sits %s cents below the Sa you gave. | तुम्ही दिलेल्या साच्या %s शतांश खाली रेकॉर्डिंग आहे. | आपने दिए सा से %s शतांश नीचे रेकॉर्डिंग है. | |
| `sa_drift_rerun` | Use the detected Sa and run again | सापडलेला सा घेऊन पुन्हा चालवा | पाए गए सा के साथ फिर से चलाइए | |
| `cd_sa_drift` | Sa may have drifted | सा सरकला असावा | सा खिसक गया हो सकता है | — spoken only |


## 15. Prahar

The database stores these as `day 2nd`; the app turns them into a phrase. A closed set of
eight, so the mapping is a lookup rather than anything clever.

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `prahar_day_1` | 1st of the day | दिवसाचा १ला | दिन का पहला | |
| `prahar_day_2` | 2nd of the day | दिवसाचा २रा | दिन का दूसरा | |
| `prahar_day_3` | 3rd of the day | दिवसाचा ३रा | दिन का तीसरा | |
| `prahar_day_4` | 4th of the day | दिवसाचा ४था | दिन का चौथा | |
| `prahar_night_1` | 1st of the night | रात्रीचा १ला | रात का पहला | |
| `prahar_night_2` | 2nd of the night | रात्रीचा २रा | रात का दूसरा | |
| `prahar_night_3` | 3rd of the night | रात्रीचा ३रा | रात का तीसरा | |
| `prahar_night_4` | 4th of the night | रात्रीचा ४था | रात का चौथा | |

## 16. Thaat

The ten thaats plus the two the database uses for raags that do not sit in one.

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `thaat_bilawal` | Bilawal | बिलावल | बिलावल | |
| `thaat_khamaj` | Khamaj | खमाज | खमाज | |
| `thaat_kafi` | Kafi | काफी | काफ़ी | |
| `thaat_asawari` | Asawari | आसावरी | आसावरी | |
| `thaat_bhairavi` | Bhairavi | भैरवी | भैरवी | |
| `thaat_bhairav` | Bhairav | भैरव | भैरव | |
| `thaat_kalyan` | Kalyan | कल्याण | कल्याण | |
| `thaat_marwa` | Marwa | मारवा | मारवा | |
| `thaat_poorvi` | Poorvi | पूर्वी | पूर्वी | |
| `thaat_todi` | Todi | तोडी | तोड़ी | |
| `thaat_carnatic` | Carnatic Music System | कर्नाटक संगीत पद्धती | कर्नाटक संगीत पद्धति | |
| `thaat_undefined` | Not Defined | अनिश्चित | अनिर्धारित | |

## 17. Playback

The raagfinders can sound out a swar or a phrase, so they carry the same Sa strip the
Record screen has.

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `cd_play_phrase` | Play %s | %s वाजवा | %s बजाइए | spoken only, the aaroha/avaroha play buttons |
| `sa_pick_done` | Done | झालं | हो गया | closes the Sa picker in the raagfinders |

## 13. Swar notation

The twelve swars, used on the By Notes circle and everywhere a raag's aaroha, avaroha,
mukhyanga or nyas swars are printed. Komal is the base swar plus U+0952 ANUDATTA, the
line *below* the letter, and teevra Ma is U+0951 UDATTA, the line above — the two marks
Unicode defines for exactly this and which both shipped fonts carry. They sit correctly
under रे and नि, where an underline would have been a workaround.

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `swar_00` | S | सा | सा | |
| `swar_01` | r | रे॒ | रे॒ | |
| `swar_02` | R | रे | रे | |
| `swar_03` | g | ग॒ | ग॒ | |
| `swar_04` | G | ग | ग | |
| `swar_05` | m | म | म | |
| `swar_06` | M | म॑ | म॑ | |
| `swar_07` | P | प | प | |
| `swar_08` | d | ध॒ | ध॒ | |
| `swar_09` | D | ध | ध | |
| `swar_10` | n | नी॒ | नि॒ | |
| `swar_11` | N | नी | नि | |
| `saptak_mandra` | %ṣ | %ṣ | %ṣ | Mandra saptak: a dot below the swar, drawn by the app rather than typed. Only the spoken form uses this string |
| `saptak_taara` | %ṡ | %ṡ | %ṡ | Taara saptak: a dot above the swar, drawn by the app rather than typed. Only the spoken form uses this string |

## 14. Raag database names

All 116 raags in the tanarang database, in the order `assets/raagdb.json` lists them
(alphabetical by English name). The 47 that overlap with section 9 reuse your corrections;
the rest are my drafts.

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `dbraag_000` | Abhogi | अभोगी | अभोगी | |
| `dbraag_001` | Adana | अडाणा | अडाना | |
| `dbraag_002` | Aheer Bhairav | अहीर भैरव | अहीर भैरव | from section 9 |
| `dbraag_003` | Alhaiya Bilawal | अल्हैय्या बिलावल | अल्हैय्या बिलावल | from section 9 |
| `dbraag_004` | Bageshree | बागेश्री | बागेश्री | from section 9 |
| `dbraag_005` | Bahar | बहार | बहार | from section 9 |
| `dbraag_006` | Bairagi | बैरागी | बैरागी | from section 9 |
| `dbraag_007` | Bairagi Todi | बैरागी तोडी | बैरागी तोड़ी | |
| `dbraag_008` | Basant | बसंत | बसंत | from section 9 |
| `dbraag_009` | Basant Mukhari | बसंत मुखारी | बसंत मुखारी | |
| `dbraag_010` | Bhairav | भैरव | भैरव | from section 9 |
| `dbraag_011` | Bhairavi | भैरवी | भैरवी | from section 9 |
| `dbraag_012` | Bhatiyar | भटियार | भटियार | |
| `dbraag_013` | Bheem | भीम | भीम | |
| `dbraag_014` | Bheempalasi | भीमपलास | भीमपलासी | from section 9 |
| `dbraag_015` | Bhoopali | भूपाळी | भूपाली | from section 9 |
| `dbraag_016` | Bhupal Todi | भूपाल तोडी | भूपाल तोड़ी | |
| `dbraag_017` | Bihag | बिहाग | बिहाग | from section 9 |
| `dbraag_018` | Bihagda | बिहागडा | बिहागड़ा | |
| `dbraag_019` | Bilaskhani Todi | बिलासखानी तोडी | बिलासखानी तोड़ी | |
| `dbraag_020` | Chandrakauns | चंद्रकंस | चंद्रकंस | from section 9 |
| `dbraag_021` | Charukeshi | चारुकेशी | चारुकेशी | from section 9 |
| `dbraag_022` | Chhayanat | छायानट | छायानट | |
| `dbraag_023` | Darbari Kanada | दरबारी कानडा | दरबारी कानड़ा | from section 9 |
| `dbraag_024` | Des | देस | देस | from section 9 |
| `dbraag_025` | Deshkar | देसकार | देसकार | from section 9 |
| `dbraag_026` | Desi | देसी | देसी | |
| `dbraag_027` | Dev Gandhar | देव गांधार | देव गांधार | |
| `dbraag_028` | Devgiri Bilawal | देवगिरी बिलावल | देवगिरी बिलावल | |
| `dbraag_029` | Devashree | देवश्री | देवश्री | |
| `dbraag_030` | Dhanashree (Bhairavi Ang) | धनाश्री (भैरवी अंग) | धनाश्री (भैरवी अंग) | |
| `dbraag_031` | Dhani | धानी | धानी | from section 9 |
| `dbraag_032` | Durga | दुर्गा | दुर्गा | from section 9 |
| `dbraag_033` | Gaud Malhar | गौड मल्हार | गौड़ मल्हार | |
| `dbraag_034` | Gaud Sarang | गौड सारंग | गौड़ सारंग | |
| `dbraag_035` | Gauri (Bhairav Ang) | गौरी (भैरव अंग) | गौरी (भैरव अंग) | |
| `dbraag_036` | Gopika Basant | गोपिका बसंत | गोपिका बसंत | |
| `dbraag_037` | Gorakh Kalyan | गोरख कल्याण | गोरख कल्याण | |
| `dbraag_038` | Gunkali | गुणकली | गुनकली | |
| `dbraag_039` | Gujari Todi | गुजरी तोडी | गुजरी तोड़ी | |
| `dbraag_040` | Hameer | हमीर | हमीर | from section 9 |
| `dbraag_041` | Hans Dhwani | हंसध्वनी | हंसध्वनि | from section 9 |
| `dbraag_042` | Hans Kinkini | हंस किंकिणी | हंस किंकिनी | |
| `dbraag_043` | Harikauns | हरिकंस | हरिकंस | |
| `dbraag_044` | Hemant | हेमंत | हेमंत | |
| `dbraag_045` | Hemshri | हेमश्री | हेमश्री | |
| `dbraag_046` | Hindol | हिंडोल | हिंडोल | from section 9 |
| `dbraag_047` | Jaijaivanti | जयजयवंती | जयजयवंती | from section 9 |
| `dbraag_048` | Jaldhar Kedar | जलधर केदार | जलधर केदार | |
| `dbraag_049` | Jaunpuri | जौनपुरी | जौनपुरी | |
| `dbraag_050` | Jayat | जयत | जयत | |
| `dbraag_051` | Jhinjhoti | झिंझोटी | झिंझोटी | |
| `dbraag_052` | Jog | जोग | जोग | from section 9 |
| `dbraag_053` | Jogeshwari | जोगेश्वरी | जोगेश्वरी | |
| `dbraag_054` | Jogiya | जोगिया | जोगिया | |
| `dbraag_055` | Jogkauns | जोगकंस | जोगकंस | |
| `dbraag_056` | Kafi | काफी | काफ़ी | from section 9 |
| `dbraag_057` | Kalawati | कलावती | कलावती | from section 9 |
| `dbraag_058` | Kamod | कामोद | कामोद | |
| `dbraag_059` | Kaushik Dhwani (Bhinn Shadj) | कौशिकध्वनी (भिन्नषड्ज) | कौशिकध्वनि (भिन्नषड्ज) | from section 9 |
| `dbraag_060` | Kausi Kanada | कौशी कानडा | कौसी कानड़ा | |
| `dbraag_061` | Kedar | केदार | केदार | from section 9 |
| `dbraag_062` | Keerwani | कीरवाणी | कीरवानी | from section 9 |
| `dbraag_063` | Khamaj | खमाज | खमाज | from section 9 |
| `dbraag_064` | Khambavati | खंबावती | खंबावती | |
| `dbraag_065` | Komal Rishabh Asawari | कोमल रिषभ आसावरी | कोमल रिषभ आसावरी | |
| `dbraag_066` | Lalit | ललित | ललित | from section 9 |
| `dbraag_067` | Lanka Dahan Sarang | लंका दहन सारंग | लंका दहन सारंग | |
| `dbraag_068` | Madhukauns | मधुकंस | मधुकंस | from section 9 |
| `dbraag_069` | Madhumad Sarang | मधुमाद सारंग | मधुमाद सारंग | |
| `dbraag_070` | Madhuvanti | मधुवंती | मधुवंती | from section 9 |
| `dbraag_071` | Malgunji | मालगुंजी | मालगुंजी | |
| `dbraag_072` | Malhar | मल्हार | मल्हार | from section 9 |
| `dbraag_073` | Malkauns | मालकंस | मालकंस | from section 9 |
| `dbraag_074` | Mand | मांड | मांड | |
| `dbraag_075` | Maru Bihag | मारुबिहाग | मारुबिहाग | from section 9 |
| `dbraag_076` | Marwa | मारवा | मारवा | from section 9 |
| `dbraag_077` | Megh | मेघ | मेघ | |
| `dbraag_078` | Mohankauns | मोहनकंस | मोहनकंस | |
| `dbraag_079` | Multani | मुलतानी | मुल्तानी | from section 9 |
| `dbraag_080` | Nand | नंद | नंद | |
| `dbraag_081` | Narayani | नारायणी | नारायनी | |
| `dbraag_082` | Nayaki Kanada | नायकी कानडा | नायकी कानड़ा | |
| `dbraag_083` | Nat Bhairav | नट भैरव | नट भैरव | |
| `dbraag_084` | Pancham Jogeshwari | पंचम जोगेश्वरी | पंचम जोगेश्वरी | |
| `dbraag_085` | Parameshwari | परमेश्वरी | परमेश्वरी | |
| `dbraag_086` | Patdeep | पटदीप | पटदीप | |
| `dbraag_087` | Pilu | पिलू | पीलू | from section 9 |
| `dbraag_088` | Poorvi | पूर्वी | पूर्वी | |
| `dbraag_089` | Puriya | पुरिया | पूरिया | |
| `dbraag_090` | Puriya Dhanashri | पुरिया धनाश्री | पूरिया धनाश्री | from section 9 |
| `dbraag_091` | Puriya Kalyan | पुरिया कल्याण | पूरिया कल्याण | from section 9 |
| `dbraag_092` | Rageshree | रागेश्री | रागेश्री | |
| `dbraag_093` | Ramdasi Malhar | रामदासी मल्हार | रामदासी मल्हार | |
| `dbraag_094` | Ramkali | रामकली | रामकली | |
| `dbraag_095` | Saalag Varali | सालग वराळी | सालग वराली | |
| `dbraag_096` | Vrindavani Sarang | वृंदावनी सारंग | वृंदावनी सारंग | from section 9 |
| `dbraag_097` | Saraswati | सरस्वती | सरस्वती | |
| `dbraag_098` | Saraswati Kedar | सरस्वती केदार | सरस्वती केदार | |
| `dbraag_099` | Shahana Kanada | शाहणा कानडा | शाहना कानड़ा | |
| `dbraag_100` | Shankara | शंकरा | शंकरा | from section 9 |
| `dbraag_101` | Shivranjani | शिवरंजनी | शिवरंजनी | from section 9 |
| `dbraag_102` | Shree | श्री | श्री | from section 9 |
| `dbraag_103` | Shuddha Kalyan | शुद्ध कल्याण | शुद्ध कल्याण | |
| `dbraag_104` | Shuddha Sarang | शुद्ध सारंग | शुद्ध सारंग | |
| `dbraag_105` | Shyam Kalyan | श्याम कल्याण | श्याम कल्याण | |
| `dbraag_106` | Sindhura | सिंधुरा | सिंधुरा | |
| `dbraag_107` | Sohani | सोहनी | सोहनी | from section 9 |
| `dbraag_108` | Sundarkauns | सुंदरकंस | सुंदरकंस | |
| `dbraag_109` | Surdasi Malhar | सूरदासी मल्हार | सूरदासी मल्हार | |
| `dbraag_110` | Tilak Kamod | तिलक कामोद | तिलक कामोद | from section 9 |
| `dbraag_111` | Tilang | तिलंग | तिलंग | from section 9 |
| `dbraag_112` | Todi | तोडी | तोड़ी | from section 9 |
| `dbraag_113` | Vachaspati | वाचस्पती | वाचस्पति | |
| `dbraag_114` | Vibhas | विभास | विभास | from section 9 |
| `dbraag_115` | Yaman | यमन | यमन | from section 9 |

## 9. Raag names

In `raags.json` order, which is the order the model emits — these are looked up by index,
so the list must not be re-sorted. The English column is the model's own label with the
CamelCase broken up, except where the common name differs from whatever the training set
called it: `KaushikDhwani` shows as Kaushik Dhwani / Bhinna Shadja, `Sarang` as Vrindavani
Sarang, and `Shivranjani` as Shivaranjani. A test pins those three so the rest still has to
match the model's order exactly.

| key | English | मराठी | हिंदी | note |
|---|---|---|---|---|
| `raag_00` | Aheer Bhairav | अहीर भैरव | अहीर भैरव |  |
| `raag_01` | Alhaiya Bilawal | अल्हैय्या बिलावल | अल्हैय्या बिलावल | |
| `raag_02` | Bageshree | बागेश्री | बागेश्री | |
| `raag_03` | Bahar | बहार | बहार | |
| `raag_04` | Bairagi | बैरागी | बैरागी | |
| `raag_05` | Basant | बसंत | बसंत | |
| `raag_06` | Bhairav | भैरव | भैरव | |
| `raag_07` | Bhairavi | भैरवी | भैरवी | |
| `raag_08` | Bheempalasi | भीमपलास | भीमपलासी | |
| `raag_09` | Bhoopali | भूपाळी | भूपाली | |
| `raag_10` | Bihag | बिहाग | बिहाग | |
| `raag_11` | Chandrakauns | चंद्रकंस | चंद्रकंस | |
| `raag_12` | Charukeshi | चारुकेशी | चारुकेशी | |
| `raag_13` | Darbari Kanada | दरबारी कानडा | दरबारी कानड़ा | |
| `raag_14` | Des | देस | देस | |
| `raag_15` | Deshkar | देसकार | देसकार | |
| `raag_16` | Dhani | धानी | धानी | |
| `raag_17` | Durga | दुर्गा | दुर्गा | |
| `raag_18` | Hameer | हमीर | हमीर | |
| `raag_19` | Hans Dhwani | हंसध्वनी | हंसध्वनि | |
| `raag_20` | Hindol | हिंडोल | हिंडोल | |
| `raag_21` | Jaijaivanti | जयजयवंती | जयजयवंती | |
| `raag_22` | Jog | जोग | जोग | |
| `raag_23` | Kafi | काफी | काफ़ी | |
| `raag_24` | Kalawati | कलावती | कलावती | |
| `raag_25` | Kaushik Dhwani / Bhinna Shadja | कौशिकध्वनी / भिन्नषड्ज | कौशिकध्वनि / भिन्नषड्ज | |
| `raag_26` | Kedar | केदार | केदार | |
| `raag_27` | Keerwani | कीरवाणी | कीरवानी | |
| `raag_28` | Khamaj | खमाज | खमाज | |
| `raag_29` | Lalit | ललित | ललित | |
| `raag_30` | Madhukauns | मधुकंस | मधुकंस | |
| `raag_31` | Madhuvanti | मधुवंती | मधुवंती | |
| `raag_32` | Malhar | मल्हार | मल्हार | |
| `raag_33` | Malkauns | मालकंस | मालकंस | |
| `raag_34` | Maru Bihag | मारुबिहाग | मारुबिहाग | |
| `raag_35` | Marwa | मारवा | मारवा | |
| `raag_36` | Multani | मुलतानी | मुल्तानी | |
| `raag_37` | Pilu | पिलू | पीलू | |
| `raag_38` | Puriya Dhanashri | पुरिया धनाश्री | पूरिया धनाश्री | |
| `raag_39` | Puriya Kalyan | पुरिया कल्याण | पूरिया कल्याण | |
| `raag_40` | Vrindavani Sarang | वृंदावनी सारंग | वृंदावनी सारंग | |
| `raag_41` | Shankara | शंकरा | शंकरा | |
| `raag_42` | Shivaranjani | शिवरंजनी | शिवरंजनी | |
| `raag_43` | Shree | श्री | श्री | |
| `raag_44` | Sohani | सोहनी | सोहनी | |
| `raag_45` | Tilak Kamod | तिलक कामोद | तिलक कामोद | |
| `raag_46` | Tilang | तिलंग | तिलंग | |
| `raag_47` | Todi | तोडी | तोड़ी | |
| `raag_48` | Vibhas | विभास | विभास | |
| `raag_49` | Yaman | यमन | यमन | |

---

## Questions I need you to settle

**1. Does the wordmark translate?** `mogra` is set in Tiro Devanagari on the home screen and
is the app's name in the launcher. Options: keep `mogra` everywhere (my default — a brand
usually stays put), or show `मोगरा` in the Marathi and Hindi builds. The type handles both.
--> `मोगरा` for Marathi/Hindi

**2. Do the 50 raag names go into Devanagari?** This is the biggest one, because it is what
the Result screen is mostly made of. `Yaman` or `यमन`, `DarbariKanada` or `दरबारी कानडा`.
Devanagari is clearly right for a Marathi or Hindi reader, but it means a second 50-name list
to check, and the model's own labels stay Latin underneath. I would do it — say the word and
I will draft the list for you to correct.
--> Yes please, Devanagari names

**3. Latin or Devanagari numerals?** I have used `20`, `50`, `5` throughout. Marathi prose
often uses `२०`, `५०`. It is a one-line switch either way, but it should be consistent, and
the Hz readouts and the timer will stay Latin regardless.
--> Devanagari numerals for Marathi; Latin for Hindi

**4. Should Kali/Safed lead in mr/hi?** Right now the Sa card reads `C♯3` large with
`Kali 1 · 138.59 Hz` underneath. For a Marathi or Hindi speaker the peg name may be the more
natural headline, with the Western note demoted. Easy to swap per language.
--> Yes, display both but `काळी १` large.

**5. What do we call a 20-second window?** The Result footer says "2 windows". The literal
*खिडक्या / खिड़कियाँ* is wrong — it is a window of time, not a window in a wall. Options:
*तुकडे / टुकड़े* (pieces), keep the English loanword, or drop the count from the footer
entirely, which is what I would do — it is developer bookkeeping and you already had me
remove it from the Analysing screen.
--> `खंड`

**6. Anything in the English that you want changed first?** Several of these read a little
stiffly in translation because the English is doing something clever — `sa_subtitle`,
`rec_keep_going_more`. Easier to fix once in three languages than three times in one.
--> made minor changes to `en` as well, along with quite a few changes to `mr` and `hi` to make it natural.
