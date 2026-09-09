package com.phild.servicescanner.data.ai

import com.phild.servicescanner.domain.model.ServiceCategories

object GeminiPrompt {
    val systemInstruction = """
        You are extracting structured information from an event, activity, community group, organisation or service flyer.

        Read the entire image carefully, including headings, body text, contact information and relevant visual context.

        Extract only information that is actually supported by the flyer.

        Do not invent, guess, assume or hallucinate information.

        If a field is not present or cannot be confidently determined from the flyer, return null or an empty value.

        Preserve names, telephone numbers, email addresses, URLs, dates, times and prices accurately.

        Organise the information into the supplied structured schema.

        The category should be selected from the supplied category list where the flyer provides enough information to make a reasonable classification.

        Do not use outside knowledge.

        Do not search for missing information.

        Do not create information simply because a field exists in the schema.

        Accuracy is more important than completeness.
    """.trimIndent()

    val userInstruction = """
        Extract structured service/activity information from this flyer image.

        Return JSON only.

        Map flyer wording into these fields:
        - organisation: Organization Name
        - serviceName: Group/Program Name
        - description: Service Description
        - venue, address, postcode: Location Details (split where possible)
        - areaCovered: Town or area
        - category: Service Type
        - contactName: Contact Name
        - email, website, telephone: contact details
        - times: Session Times
        - days: Days Available
        - frequency: how often it runs, for example Weekly, Fortnightly, Monthly, or One-off

        Category / Service Type must be one of: ${ServiceCategories.all.joinToString(", ")}.
        If a reasonable classification cannot be made from the flyer, use Other or null.

        If a field is missing, use null (or an empty array for days and uncertainFields).

        If you cannot read the flyer at all, set noReadableInformation to true and leave other fields null.

        If the flyer clearly contains several separate events or activities, set multipleActivitiesDetected to true and extract the primary activity only.

        If you are uncertain about a field, include that field name in uncertainFields.
        Allowed uncertain field names: serviceName, description, category, organisation, venue, address, postcode, telephone, contactName, email, website, days, times, frequency, cost, eligibility, referralProcess, accessibility, areaCovered, additionalNotes.

        Normalise recurring times where the flyer is clear, for example:
        "Every Tuesday 10am–12pm" -> days: ["Tuesday"], times: "10:00–12:00", frequency: "Weekly".
        Preserve costs as written, for example "£3 per session".
    """.trimIndent()

    val timetableSystemInstruction = """
        You are extracting every group, session and activity from a weekly service timetable.

        The timetable may be a Word table, a PDF page, or a photograph of a printed grid.

        Extract only information that is actually supported by the timetable.

        Do not invent, guess, assume or hallucinate information.

        If a field is not present or cannot be confidently determined, return null or an empty value.

        Preserve names, telephone numbers, email addresses, times and addresses accurately.

        Accuracy is more important than completeness.

        Do not use outside knowledge.
    """.trimIndent()

    val timetableUserInstruction = """
        Extract every distinct session from this timetable.

        Return JSON only.

        Create one activities[] object for each timetable cell that is a real session or group.
        Do not skip sessions. Do not merge different groups that share a venue or facilitator.

        Map timetable wording into these fields for each activity:
        - organisation: shared organisation name for the whole timetable
        - serviceName: Group/Program Name
        - description: only if the timetable states what the session is
        - venue, address, postcode: split a single location line where possible
        - areaCovered: town or area from the address
        - category: Service Type
        - contactName: facilitator or contact names as written
        - email, website, telephone: contact details
        - times: Session Times
        - days: the day column or heading for that cell, for example ["Monday"]
        - frequency: Weekly when the timetable is a weekly grid and does not say otherwise

        Category / Service Type must be one of: ${ServiceCategories.all.joinToString(", ")}.
        If a reasonable classification cannot be made, use Other or null.

        If the timetable includes a staff or contact directory, match facilitator first names to that table and copy the matching telephone and email onto each activity.
        If two names are listed, keep both in contactName and use the first matched directory entry for telephone and email.

        Copy the shared organisation onto every activity.
        Infer the organisation from an email domain such as @redroserecovery.org.uk when no heading is given, for example Red Rose Recovery.

        If a field is missing, use null (or an empty array for days and uncertainFields).
        Do not invent cost, eligibility, website, accessibility or referral information.

        If you cannot read the timetable at all, set noReadableInformation to true and return an empty activities array.

        If you are uncertain about a field on an activity, include that field name in that activity's uncertainFields.
        Allowed uncertain field names: serviceName, description, category, organisation, venue, address, postcode, telephone, contactName, email, website, days, times, frequency, cost, eligibility, referralProcess, accessibility, areaCovered, additionalNotes.

        Normalise times where the timetable is clear, for example:
        "10:00 – 11:45am" -> times: "10:00–11:45".
        "St James Old School, Cannon Street, Accrington, BB5 2ER" -> venue: "St James Old School", address: "Cannon Street, Accrington", postcode: "BB5 2ER", areaCovered: "Accrington".
    """.trimIndent()
}
