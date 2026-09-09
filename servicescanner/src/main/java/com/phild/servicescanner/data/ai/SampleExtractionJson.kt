package com.phild.servicescanner.data.ai

object SampleExtractionJson {
    const val DEFAULT = """
    {
      "serviceName": "Burnley Walking Group",
      "description": "A friendly weekly walk for adults who want to get active and meet others.",
      "category": "Social Activity",
      "organisation": "Burnley Community Health",
      "venue": "Towneley Park Visitor Centre",
      "address": "Towneley Park, Burnley",
      "postcode": null,
      "telephone": "01282 123456",
      "contactName": "Walk Coordinator",
      "email": null,
      "website": "https://example.org/walking",
      "days": ["Tuesday"],
      "times": "10:00–12:00",
      "frequency": "Weekly",
      "cost": "Free",
      "eligibility": null,
      "referralProcess": null,
      "accessibility": null,
      "areaCovered": "Burnley",
      "additionalNotes": "Meet at the visitor centre entrance. Wear suitable shoes.",
      "uncertainFields": ["telephone"],
      "multipleActivitiesDetected": false
    }
    """

    const val TIMETABLE = """
    {
      "activities": [
        {
          "serviceName": "Get crafty – arts and crafts",
          "description": null,
          "category": "Community Activity",
          "organisation": "Red Rose Recovery",
          "venue": "St James Old School",
          "address": "Cannon Street, Accrington",
          "postcode": "BB5 2ER",
          "telephone": "07483356858",
          "contactName": "Bridget",
          "email": "Bridget.holden@redroserecovery.org.uk",
          "website": null,
          "days": ["Monday"],
          "times": "10:00–11:45",
          "frequency": "Weekly",
          "cost": null,
          "eligibility": null,
          "referralProcess": null,
          "accessibility": null,
          "areaCovered": "Accrington",
          "additionalNotes": null,
          "uncertainFields": []
        },
        {
          "serviceName": "Community café",
          "description": null,
          "category": "Social Activity",
          "organisation": "Red Rose Recovery",
          "venue": "Double Decker café",
          "address": "8-10 Church Street, Accrington",
          "postcode": "BB5 2EH",
          "telephone": "07483915707",
          "contactName": "Gemma",
          "email": "Gemma.mulholland@redroserecovery.org.uk",
          "website": null,
          "days": ["Tuesday"],
          "times": "10:00–11:30",
          "frequency": "Weekly",
          "cost": null,
          "eligibility": null,
          "referralProcess": null,
          "accessibility": null,
          "areaCovered": "Accrington",
          "additionalNotes": null,
          "uncertainFields": []
        }
      ],
      "noReadableInformation": false
    }
    """
}
