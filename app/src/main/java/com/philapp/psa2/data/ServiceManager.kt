package com.philapp.psa2.data

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.philapp.psa2.model.Service
import com.philapp.psa2.model.ServiceType
import com.philapp.psa2.model.ServiceStatus
import com.philapp.psa2.model.ContactInfo
import android.util.Log
import com.philapp.psa2.utils.generateServiceId

object ServiceManager {

    private val hardcodedServices = listOf(
        // Original 5 services with town field added
        Service(
            id = "burnley_together",
            organizationName = "Burnley Together",
            groupName = "Emergency Food Support",
            location = "Valley Street Community Centre, BB11 5LZ",
            town = "Burnley",
            description = "Emergency food parcels and community grocery support for those in need.",
            types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
            features = listOf("Food parcels", "Community grocery", "Emergency support"),
            contact = ContactInfo(
                phone = "01282 686402",
                email = "contact@burnleytogether.org.uk"
            ),
            schedule = "Monday - Friday, 09:00 - 16:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "blackburn_mental_health",
            organizationName = "Blackburn Mental Health Support",
            groupName = "Peer Support Group",
            location = "Blackburn Community Centre, BB1 1AA",
            town = "Blackburn",
            description = "Weekly peer support group for mental health recovery and wellbeing.",
            types = listOf(ServiceType.MENTAL_HEALTH, ServiceType.PEER_SUPPORT),
            features = listOf("Peer support", "Mental health", "Recovery focused"),
            contact = ContactInfo(
                phone = "01254 123456",
                email = "support@blackburnmentalhealth.org"
            ),
            schedule = "Every Tuesday, 18:00 - 20:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "accrington_sports",
            organizationName = "Accrington Sports Club",
            groupName = "Community Fitness",
            location = "Accrington Sports Centre, BB5 1AA",
            town = "Accrington",
            description = "Inclusive sports and fitness activities for all abilities and ages.",
            types = listOf(ServiceType.SPORT_AND_FITNESS, ServiceType.SOCIAL),
            features = listOf("Fitness classes", "Team sports", "Inclusive activities"),
            contact = ContactInfo(
                phone = "01254 987654",
                email = "info@accringtonsports.org"
            ),
            schedule = "Monday, Wednesday, Friday, 10:00 - 12:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "nelson_skills",
            organizationName = "Nelson Skills Development",
            groupName = "Employment Training",
            location = "Nelson Training Centre, BB9 1AA",
            town = "Nelson",
            description = "Skills development and employment training for local residents.",
            types = listOf(ServiceType.EMPLOYMENT, ServiceType.SKILL_BUILDING),
            features = listOf("Job training", "Skills development", "Employment support"),
            contact = ContactInfo(
                phone = "01282 456789",
                email = "training@nelson-skills.org"
            ),
            schedule = "Tuesday - Thursday, 09:00 - 15:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "rawtenstall_community",
            organizationName = "Rawtenstall Community Hub",
            groupName = "Social Activities",
            location = "Rawtenstall Community Hall, BB4 1AA",
            town = "Rawtenstall",
            description = "Community hub offering various social activities and support groups.",
            types = listOf(ServiceType.SOCIAL, ServiceType.COMMUNITY_INTEREST_GROUPS),
            features = listOf("Social activities", "Community groups", "Support networks"),
            contact = ContactInfo(
                phone = "01706 123456",
                email = "hello@rawtenstallcommunity.org"
            ),
            schedule = "Various times throughout the week",
            status = ServiceStatus.APPROVED
        ),
        
        // Pendle YES Hub services (8 services)
        Service(
            id = "pendle_yes_hub_guitar_advanced",
            organizationName = "Pendle YES Hub",
            groupName = "Don't Fret: Guitar Lessons (Advanced)",
            location = "Pendle YES Hub, Nelson",
            town = "Nelson",
            description = "Advanced guitar lessons with Aaron",
            types = listOf(ServiceType.SOCIAL),
            features = emptyList(),
            contact = ContactInfo(
                phone = "07859739635",
                email = "DMarshall@activelancashire.org.uk"
            ),
            schedule = "Monday 13:00 – 14:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "pendle_yes_hub_national_careers_support",
            organizationName = "Pendle YES Hub",
            groupName = "National Careers Service Employment Support",
            location = "Pendle YES Hub, Nelson",
            town = "Nelson",
            description = "Employment support with the National Careers Service",
            types = listOf(ServiceType.EMPLOYMENT),
            features = emptyList(),
            contact = ContactInfo(
                phone = "07859739635",
                email = "DMarshall@activelancashire.org.uk"
            ),
            schedule = "Tuesday 09:00 – 16:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "pendle_yes_hub_group_sports",
            organizationName = "Pendle YES Hub",
            groupName = "Pickleball, Badminton and Football",
            location = "Leisure Box, BB9 5NH, Nelson",
            town = "Nelson",
            description = "Group sports sessions including pickleball, badminton and football",
            types = listOf(ServiceType.SPORT_AND_FITNESS),
            features = emptyList(),
            contact = ContactInfo(
                phone = "07859739635",
                email = "DMarshall@activelancashire.org.uk"
            ),
            schedule = "Tuesday 16:00 – 17:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "pendle_yes_hub_walking_wednesdays",
            organizationName = "Pendle YES Hub",
            groupName = "Walking Wednesdays",
            location = "Pendle YES Hub, Nelson",
            town = "Nelson",
            description = "Local walks for health and socialising",
            types = listOf(ServiceType.SPORT_AND_FITNESS),
            features = emptyList(),
            contact = ContactInfo(
                phone = "07859739635",
                email = "DMarshall@activelancashire.org.uk"
            ),
            schedule = "Wednesday 13:00 – 14:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "pendle_yes_hub_mental_health_1to1",
            organizationName = "Pendle YES Hub",
            groupName = "1-1 Mental Health Wellbeing Support (Kieran and Sarah)",
            location = "Pendle YES Hub, Nelson",
            town = "Nelson",
            description = "One-to-one mental health wellbeing support",
            types = listOf(ServiceType.MENTAL_HEALTH),
            features = emptyList(),
            contact = ContactInfo(
                phone = "07859739635",
                email = "DMarshall@activelancashire.org.uk"
            ),
            schedule = "Wednesday 12:00 – 15:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "pendle_yes_hub_snooker_pool",
            organizationName = "Pendle YES Hub",
            groupName = "Snooker and Pool",
            location = "Alexandra Snooker Club, 5 Holme Street, Nelson",
            town = "Nelson",
            description = "Snooker and pool session",
            types = listOf(ServiceType.SOCIAL),
            features = emptyList(),
            contact = ContactInfo(
                phone = "07859739635",
                email = "DMarshall@activelancashire.org.uk"
            ),
            schedule = "Friday 12:00 – 13:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "pendle_yes_hub_burnley_college_support",
            organizationName = "Pendle YES Hub",
            groupName = "Burnley College Employment & Courses Support",
            location = "Pendle YES Hub, Nelson",
            town = "Nelson",
            description = "Employment and course support from Burnley College",
            types = listOf(ServiceType.EMPLOYMENT, ServiceType.EDUCATION),
            features = emptyList(),
            contact = ContactInfo(
                phone = "07859739635",
                email = "DMarshall@activelancashire.org.uk"
            ),
            schedule = "Thursday 13:30 – 16:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "pendle_yes_hub_gym_session",
            organizationName = "Pendle YES Hub",
            groupName = "Gym Session",
            location = "Pendle Wavelengths, BB9 9TD, Nelson",
            town = "Nelson",
            description = "Group gym session",
            types = listOf(ServiceType.SPORT_AND_FITNESS),
            features = emptyList(),
            contact = ContactInfo(
                phone = "07859739635",
                email = "DMarshall@activelancashire.org.uk"
            ),
            schedule = "Thursday 14:00 – 15:00",
            status = ServiceStatus.APPROVED
        ),
        
        // Red Rose Recovery services (10 services)
        Service(
            id = "red_rose_recovery_funday_monday",
            organizationName = "Red Rose Recovery",
            groupName = "Funday Monday",
            location = "St.James Old School Building, Accrington",
            town = "Accrington",
            description = "Light-hearted bingo with peers",
            types = listOf(ServiceType.SOCIAL),
            features = listOf("Bingo", "Social", "Peer Support"),
            contact = ContactInfo(
                phone = "Bridget - 07483356858",
                email = ""
            ),
            schedule = "Monday 13:30-14:30 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://redroserecovery.org.uk"
        ),
        Service(
            id = "red_rose_recovery_mindful_movement",
            organizationName = "Red Rose Recovery",
            groupName = "Mindful Movement",
            location = "St.James Old School Building, Accrington",
            town = "Accrington",
            description = "Calm your mind with relaxing movements and light exercise, recovery based.",
            types = listOf(ServiceType.SPORT_AND_FITNESS, ServiceType.RECOVERY),
            features = listOf("Mindfulness", "Exercise", "Recovery Support"),
            contact = ContactInfo(
                phone = "Bridget - 07483356858",
                email = ""
            ),
            schedule = "Monday 14:30-15:30 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://redroserecovery.org.uk"
        ),
        Service(
            id = "red_rose_recovery_community_cafe",
            organizationName = "Red Rose Recovery",
            groupName = "Community Cafe",
            location = "St.James Old School Building, Accrington",
            town = "Accrington",
            description = "Coffee and Chat with others in recovery",
            types = listOf(ServiceType.SOCIAL, ServiceType.PEER_SUPPORT),
            features = listOf("Coffee", "Social", "Peer Support"),
            contact = ContactInfo(
                phone = "Gemma",
                email = ""
            ),
            schedule = "Tuesday 10:00-11:30 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://redroserecovery.org.uk"
        ),
        Service(
            id = "red_rose_recovery_lunch_brunch_walk_talk",
            organizationName = "Red Rose Recovery",
            groupName = "Lunch, Brunch, Walk and Talk",
            location = "ABD Centre, Burnley Road, Bacup",
            town = "Bacup",
            description = "Come along for breakfast and a lovely walk through the countryside. Pick-ups can be arranged from fixed locations.",
            types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS),
            features = listOf("Breakfast", "Walking", "Transport available", "Social"),
            contact = ContactInfo(
                phone = "Shaun - 07351614902",
                email = ""
            ),
            schedule = "Tuesday 11:00-14:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://redroserecovery.org.uk"
        ),
        Service(
            id = "red_rose_recovery_here_now",
            organizationName = "Red Rose Recovery",
            groupName = "Here & Now",
            location = "St.James Old School Building, Accrington",
            town = "Accrington",
            description = "Peer support with lived experience facilitators, Recovery Based.",
            types = listOf(ServiceType.PEER_SUPPORT),
            features = listOf("Peer Support", "Recovery Based", "Lived Experience"),
            contact = ContactInfo(
                phone = "Gemma - 07483915707",
                email = ""
            ),
            schedule = "Tuesday 12:30-13:30 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://redroserecovery.org.uk"
        ),
        Service(
            id = "red_rose_recovery_feel_good_fitness",
            organizationName = "Red Rose Recovery",
            groupName = "Feel Good Fitness",
            location = "St.James Old School Building, Accrington",
            town = "Accrington",
            description = "Light exercise suitable for all abilities",
            types = listOf(ServiceType.SPORT_AND_FITNESS, ServiceType.RECOVERY, ServiceType.SOCIAL),
            features = listOf("Exercise", "All abilities", "Recovery Support"),
            contact = ContactInfo(
                phone = "Bridget - 07483356858",
                email = ""
            ),
            schedule = "Tuesday 14:00-15:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://redroserecovery.org.uk"
        ),
        Service(
            id = "red_rose_recovery_fishing_mental_health",
            organizationName = "Red Rose Recovery",
            groupName = "Fishing for Mental Health",
            location = "Burnley Inspire East Lancashire, Westgate, BB111RY",
            town = "Burnley",
            description = "Fishing Group, Recovery Based, Pick-ups from fixed locations. Bookings must be made with Shaun or Gareth.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.RECOVERY, ServiceType.MENTAL_HEALTH, ServiceType.SKILL_BUILDING),
            features = listOf("Fishing", "Transport available", "Booking required", "Recovery Support"),
            contact = ContactInfo(
                phone = "Shaun - 07351614902, Gareth - 07351614926",
                email = ""
            ),
            schedule = "Wednesday 09:00-14:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://redroserecovery.org.uk"
        ),
        Service(
            id = "red_rose_recovery_5_ways_wellbeing",
            organizationName = "Red Rose Recovery",
            groupName = "5 Ways to Wellbeing",
            location = "St.James Old School Building, Accrington",
            town = "Accrington",
            description = "Peer support Group based on 5 ways of wellbeing, Recovery Based",
            types = listOf(ServiceType.PEER_SUPPORT, ServiceType.RECOVERY, ServiceType.SKILL_BUILDING),
            features = listOf("Wellbeing", "Peer Support", "Recovery Based"),
            contact = ContactInfo(
                phone = "Emma - 07852221505",
                email = ""
            ),
            schedule = "Wednesday 10:00-11:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://redroserecovery.org.uk"
        ),
        Service(
            id = "red_rose_recovery_no_excuse_boxing",
            organizationName = "Red Rose Recovery",
            groupName = "No Excuse Boxing Workout",
            location = "87 Blackburn Road, Accrington",
            town = "Accrington",
            description = "Improve your fitness in a friendly environment with other people in recovery from drugs/alcohol/mental health issues.",
            types = listOf(ServiceType.SPORT_AND_FITNESS, ServiceType.RECOVERY),
            features = listOf("Boxing", "Fitness", "Recovery Support", "Group Workout"),
            contact = ContactInfo(
                phone = "Gemma - 07483915707",
                email = ""
            ),
            schedule = "Monday 11:45-13:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://redroserecovery.org.uk"
        ),
        Service(
            id = "red_rose_recovery_get_crafty",
            organizationName = "Red Rose Recovery",
            groupName = "Get Crafty",
            location = "St.James Old School Building, Accrington",
            town = "Accrington",
            description = "Arts And Crafts",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.PEER_SUPPORT),
            features = listOf("Arts and Crafts", "Social", "Peer Support"),
            contact = ContactInfo(
                phone = "Bridget - 07483356858",
                email = ""
            ),
            schedule = "Monday 10:00-11:45 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://redroserecovery.org.uk"
        ),
        
        // Inspire Services (Wednesday, Thursday, Friday) - 11 services
        Service(
            id = "inspire_womens_coffee_group",
            organizationName = "Inspire",
            groupName = "Women's Coffee Group with Jodie",
            location = "Inspire Location",
            town = "Burnley",
            description = "Women's coffee group session",
            types = listOf(ServiceType.SOCIAL, ServiceType.PEER_SUPPORT),
            features = listOf("Women only", "Coffee group"),
            contact = ContactInfo(phone = "", email = ""),
            schedule = "Wednesday 10:00 to 13:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "inspire_fishing_mental_health",
            organizationName = "Red Rose Recovery/Inspire",
            groupName = "Fishing for mental health",
            location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
            town = "Burnley",
            description = "Inclusive fishing group & lessons. Booking must be made as spaces are limited.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.RECOVERY, ServiceType.PEER_SUPPORT),
            features = listOf("Fishing", "Lessons", "Booking required"),
            contact = ContactInfo(phone = "07727611550", email = ""),
            schedule = "Wednesday 09:15 to 14:30",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "inspire_recovery_social_evening",
            organizationName = "Inspire",
            groupName = "Recovery Social Evening",
            location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
            town = "Burnley",
            description = "A friendly get together for a bit of food, a bit of fun and a bit of banter.",
            types = listOf(ServiceType.PEER_SUPPORT, ServiceType.RECOVERY),
            features = listOf("Social", "Food", "Entertainment"),
            contact = ContactInfo(phone = "", email = ""),
            schedule = "Wednesday 17:00 to 19:30 (Last Wednesday of month)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "inspire_cooking_budget",
            organizationName = "Inspire",
            groupName = "Cooking on a budget",
            location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
            town = "Burnley",
            description = "Cooking class followed by lunch",
            types = listOf(ServiceType.SOCIAL, ServiceType.SKILL_BUILDING),
            features = listOf("Cooking", "Lunch provided", "Budget friendly"),
            contact = ContactInfo(phone = "", email = ""),
            schedule = "Thursday 10:30 to 12:30",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "inspire_mens_group",
            organizationName = "Inspire",
            groupName = "Men's Group",
            location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
            town = "Burnley",
            description = "Men's group session",
            types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL),
            features = listOf("Men only", "Peer support"),
            contact = ContactInfo(phone = "", email = ""),
            schedule = "Thursday 13:00 to 14:30",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "inspire_walking_group",
            organizationName = "Inspire",
            groupName = "Walking Group",
            location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
            town = "Burnley",
            description = "Walking group for physical and mental wellbeing",
            types = listOf(ServiceType.SPORT_AND_FITNESS, ServiceType.SOCIAL),
            features = listOf("Walking", "Exercise", "Social"),
            contact = ContactInfo(phone = "", email = ""),
            schedule = "Thursday 14:00 to 15:30",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "inspire_breakfast_club",
            organizationName = "Inspire",
            groupName = "Breakfast Club",
            location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
            town = "Burnley",
            description = "Breakfast Club for those participating in groups",
            types = listOf(ServiceType.SOCIAL, ServiceType.PEER_SUPPORT),
            features = listOf("Breakfast", "Social"),
            contact = ContactInfo(phone = "", email = ""),
            schedule = "Friday 09:00 to 10:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "inspire_declutter_mind_course",
            organizationName = "Inspire",
            groupName = "Declutter Your Mind Course",
            location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
            town = "Burnley",
            description = "Mental wellness and mindfulness course",
            types = listOf(ServiceType.MENTAL_HEALTH, ServiceType.PEER_SUPPORT),
            features = listOf("Mindfulness", "Mental wellness", "Course"),
            contact = ContactInfo(phone = "", email = ""),
            schedule = "Friday 10:30 to 12:30",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "inspire_fun_time_friday",
            organizationName = "Inspire",
            groupName = "Fun Time Friday",
            location = "Inspire, Burnley house, 37-41 Westgate, Burnley, BB11 1RY",
            town = "Burnley",
            description = "Social Group - Burnley inspire",
            types = listOf(ServiceType.SOCIAL, ServiceType.PEER_SUPPORT),
            features = listOf("Social", "Fun activities"),
            contact = ContactInfo(phone = "", email = ""),
            schedule = "Friday 13:00 to 14:30",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "inspire_arts_crafts_bekki",
            organizationName = "Inspire",
            groupName = "Arts & Crafts with Bekki",
            location = "Grassroots Nelson",
            town = "Nelson",
            description = "Arts and crafts session",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL),
            features = listOf("Art", "Crafts", "Creative"),
            contact = ContactInfo(phone = "", email = ""),
            schedule = "Friday 13:30 to 15:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "inspire_here_now_bryan",
            organizationName = "Inspire",
            groupName = "Here and Now Group with Bryan",
            location = "Accrington Inspire",
            town = "Accrington",
            description = "Mindfulness and present-moment awareness group",
            types = listOf(ServiceType.MENTAL_HEALTH, ServiceType.PEER_SUPPORT),
            features = listOf("Mindfulness", "Mental wellness"),
            contact = ContactInfo(phone = "", email = ""),
            schedule = "Friday 14:00 to 15:00",
            status = ServiceStatus.APPROVED
        ),
        
        // Additional Inspire Services from getSampleServices - 5 services
        Service(
            id = "inspire_woodnook_breakfast_club",
            organizationName = "Inspire",
            groupName = "Woodnook Breakfast Club",
            location = "Woodnook Community Centre, Royd Street, Accrington, BB5 2JH",
            town = "Accrington",
            description = "Breakfast club for community members",
            types = listOf(ServiceType.SOCIAL, ServiceType.MENTAL_HEALTH, ServiceType.PEER_SUPPORT),
            features = listOf("Breakfast", "Social", "Support"),
            contact = ContactInfo(phone = "", email = ""),
            schedule = "Tuesday 09:30-10:30",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://inspirelancs.org.uk/east-lancs/"
        ),
        Service(
            id = "inspire_drop_in",
            organizationName = "Inspire",
            groupName = "Drop-in",
            location = "The Zone, New Era, Accrington, BB5 1PB",
            town = "Accrington",
            description = "Come along for a friendly chat, grab a brew and a biscuit, and talk things over in a supportive environment",
            types = listOf(ServiceType.PEER_SUPPORT),
            features = listOf("Drop-in", "Social", "Support", "Refreshments"),
            contact = ContactInfo(phone = "", email = ""),
            schedule = "Tuesday 11:00-13:00",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://inspirelancs.org.uk/east-lancs/"
        ),
        Service(
            id = "inspire_brunch_club",
            organizationName = "Inspire",
            groupName = "Brunch Club",
            location = "A B & D Centre, Burnley Road, Bacup, OL13 8AB",
            town = "Bacup",
            description = "Free food, free advice, and friendly, supportive company",
            types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL),
            features = listOf("Food", "Advice", "Support", "Social"),
            contact = ContactInfo(phone = "", email = ""),
            schedule = "Tuesday 11:00-12:00",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://inspirelancs.org.uk/east-lancs/"
        ),
        Service(
            id = "inspire_walking_group_bacup",
            organizationName = "Inspire",
            groupName = "Walking group Bacup",
            location = "A B & D Centre, Burnley Road, Bacup, OL13 8AB",
            town = "Bacup",
            description = "Walking group with friendly folk. All abilities welcome!",
            types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS),
            features = listOf("Walking", "Exercise", "Social", "Inclusive"),
            contact = ContactInfo(phone = "", email = ""),
            schedule = "Tuesday 12:00-14:00",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://inspirelancs.org.uk/east-lancs/"
        ),
        Service(
            id = "inspire_acupuncture",
            organizationName = "Inspire",
            groupName = "Acupuncture",
            location = "Inspire, 33 Eagle Street, Accrington, BB5 1LN",
            town = "Accrington",
            description = "Acupuncture treatment for wellbeing",
            types = listOf(ServiceType.MENTAL_HEALTH),
            features = listOf("Acupuncture", "Wellbeing"),
            contact = ContactInfo(phone = "Please ask your key-worker for details", email = ""),
            schedule = "Tuesday 12:50",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://inspirelancs.org.uk/east-lancs/"
        ),
        
        // Colne Services - 3 services
        Service(
            id = "colne_walking_group",
            organizationName = "Colne Community Centre",
            groupName = "Colne Walking Group",
            location = "Colne Community Centre, Colne",
            town = "Colne",
            description = "Weekly walking group exploring the beautiful countryside around Colne. All abilities welcome!",
            types = listOf(ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS, ServiceType.PEER_SUPPORT),
            features = listOf("Walking", "Outdoor", "Social", "All abilities"),
            contact = ContactInfo(
                phone = "01282 123456",
                email = "info@colnecommunity.org"
            ),
            schedule = "Tuesday 10:00-12:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "colne_wellbeing_group",
            organizationName = "Colne Mental Health Support",
            groupName = "Colne Wellbeing Group",
            location = "Colne Library, Market Street, Colne",
            town = "Colne",
            description = "A supportive group for people experiencing mental health challenges. Share experiences and learn coping strategies.",
            types = listOf(ServiceType.MENTAL_HEALTH, ServiceType.PEER_SUPPORT),
            features = listOf("Mental health support", "Peer support", "Coping strategies", "Safe space"),
            contact = ContactInfo(
                phone = "01282 654321",
                email = "wellbeing@colne.org"
            ),
            schedule = "Thursday 14:00-16:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "colne_fitness_club",
            organizationName = "Colne Sports Centre",
            groupName = "Colne Fitness Club",
            location = "Colne Sports Centre, Colne",
            town = "Colne",
            description = "Fitness sessions suitable for all levels. Improve your health and meet new people in a friendly environment.",
            types = listOf(ServiceType.SPORT_AND_FITNESS, ServiceType.SOCIAL),
            features = listOf("Fitness", "Exercise", "Social", "All levels"),
            contact = ContactInfo(
                phone = "01282 789012",
                email = "fitness@colnesports.org"
            ),
            schedule = "Monday and Wednesday 18:00-19:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        
        // Practical Support Services - 4 services
        Service(
            id = "lancashire_food_bank_accrington",
            organizationName = "Lancashire Food Bank",
            groupName = "Accrington Food Bank",
            location = "Accrington Community Centre, Accrington",
            town = "Accrington",
            description = "Emergency food support for individuals and families in need. No referral required, confidential service.",
            types = listOf(ServiceType.PRACTICAL, ServiceType.FOOD_BANKS),
            features = listOf("Emergency food", "No referral needed", "Confidential", "Family support"),
            contact = ContactInfo(
                phone = "01282 456789",
                email = "accrington@lancashirefoodbank.org"
            ),
            schedule = "Monday, Wednesday, Friday 10:00-14:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "lancashire_food_bank_burnley",
            organizationName = "Lancashire Food Bank",
            groupName = "Burnley Food Bank",
            location = "Burnley Community Hub, Burnley",
            town = "Burnley",
            description = "Food bank providing essential supplies to those experiencing food poverty. Referral system available.",
            types = listOf(ServiceType.PRACTICAL, ServiceType.FOOD_BANKS),
            features = listOf("Essential supplies", "Referral system", "Emergency support", "Community hub"),
            contact = ContactInfo(
                phone = "01282 789456",
                email = "burnley@lancashirefoodbank.org"
            ),
            schedule = "Tuesday, Thursday 09:00-15:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "lancashire_care_services",
            organizationName = "Lancashire Care Services",
            groupName = "Home Care Support",
            location = "Various locations across Lancashire",
            town = "Lancashire",
            description = "Professional home care services including personal care, domestic support, and companionship for elderly and vulnerable individuals.",
            types = listOf(ServiceType.PRACTICAL),
            features = listOf("Personal care", "Domestic support", "Companionship", "Elderly care", "Vulnerable support"),
            contact = ContactInfo(
                phone = "0800 123 4567",
                email = "info@lancashirecare.org"
            ),
            schedule = "Monday to Sunday, 24/7 availability",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "community_transport_service",
            organizationName = "Community Transport Service",
            groupName = "Lancashire Community Transport",
            location = "Various pick-up points across Lancashire",
            town = "Lancashire",
            description = "Accessible transport service for medical appointments, shopping, and social activities. Wheelchair accessible vehicles available.",
            types = listOf(ServiceType.PRACTICAL),
            features = listOf("Medical transport", "Shopping trips", "Wheelchair accessible", "Social outings", "Door-to-door service"),
            contact = ContactInfo(
                phone = "01282 321654",
                email = "transport@lancashirecommunity.org"
            ),
            schedule = "Monday to Friday 08:00-18:00",
            status = ServiceStatus.APPROVED
        ),
        
        // Community Interest Groups - 3 services
        Service(
            id = "lancashire_creative_writing",
            organizationName = "Lancashire Creative Writing Group",
            groupName = "Creative Writing Workshop",
            location = "Accrington Library, St James Street, Accrington",
            town = "Accrington",
            description = "Join our creative writing group to explore storytelling, poetry, and creative expression. All levels welcome from beginners to experienced writers.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SKILL_BUILDING),
            features = listOf("Creative writing", "Poetry", "Storytelling", "All levels", "Workshop format"),
            contact = ContactInfo(
                phone = "01254 123456",
                email = "writing@lancashirecreative.org"
            ),
            schedule = "Tuesday 14:00-16:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "burnley_music_society",
            organizationName = "Burnley Music Society",
            groupName = "Community Choir",
            location = "Burnley Community Centre, Burnley",
            town = "Burnley",
            description = "Join our friendly community choir. No experience necessary - just bring your voice and enthusiasm! We sing a variety of music from folk to contemporary.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL),
            features = listOf("Choir", "Music", "Singing", "No experience needed", "Social"),
            contact = ContactInfo(
                phone = "01282 654321",
                email = "choir@burnleymusic.org"
            ),
            schedule = "Thursday 19:00-21:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "colne_photography_club",
            organizationName = "Colne Photography Club",
            groupName = "Photography for Beginners",
            location = "Colne Community Centre, Colne",
            town = "Colne",
            description = "Learn photography basics and improve your skills. Bring your camera or smartphone. We cover composition, lighting, and editing techniques.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SKILL_BUILDING),
            features = listOf("Photography", "Beginners welcome", "Camera skills", "Editing", "Outdoor sessions"),
            contact = ContactInfo(
                phone = "01282 789123",
                email = "photo@colnecommunity.org"
            ),
            schedule = "Saturday 10:00-12:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        
        // Food Bank Services - 11 services
        Service(
            id = "burnley_pendle_food_bank_ghausia",
            organizationName = "Burnley & Pendle Food Bank Group",
            groupName = "Ghausia Food Bank",
            location = "Burnley",
            town = "Burnley",
            description = "Deliver food packages in Burnley",
            types = listOf(ServiceType.PRACTICAL),
            features = listOf("Food Bank", "Delivery available"),
            contact = ContactInfo(phone = "07449559459", email = ""),
            schedule = "Monday to Friday (Daily)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "burnley_together_bfcitc_foodbank",
            organizationName = "Burnley Together",
            groupName = "BFCitC Foodbank / Community Grocery",
            location = "Valley Street Community Centre, BB11 5LZ and Charter Walk (above New Look), BB11 1QJ",
            town = "Burnley",
            description = "Emergency food parcels and low-cost food membership scheme. Community Grocery offers affordable weekly food shopping for members.",
            types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
            features = listOf("Emergency food parcels", "Low-cost membership", "Community grocery", "Weekly shopping"),
            contact = ContactInfo(phone = "01282 686402", email = "contact@burnleytogether.org.uk"),
            schedule = "Community Grocery: 09:00 – 16:00 (times vary by location), Monday to Friday",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "church_street_ministries_inspiring_grace",
            organizationName = "Church on the Street Ministries",
            groupName = "Inspiring Grace Foodbank",
            location = "B C Church, Adamson Street, Burnley, BB12 6RB",
            town = "Burnley",
            description = "Provides food parcels and support, including deliveries to Burnley & Pendle residents.",
            types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
            features = listOf("Food parcels", "Delivery service", "Support available"),
            contact = ContactInfo(phone = "07582 776574", email = "Michaelflemmingaim@gmail.com"),
            schedule = "By arrangement, as needed (call to confirm)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "salvation_army_burnley_food_support",
            organizationName = "The Salvation Army",
            groupName = "Burnley Salvation Army Food Support",
            location = "Richard Street, Burnley, BB11 3AJ",
            town = "Burnley",
            description = "Community assistance, including potential food parcel provision.",
            types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
            features = listOf("Community assistance", "Food parcels", "Support services"),
            contact = ContactInfo(phone = "01282 415840 / 01282 425588", email = "lorraine.oneill@salvationarmy.org.uk"),
            schedule = "By arrangement, contact for details",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "raft_foundation_food_bank",
            organizationName = "RAFT Foundation",
            groupName = "RAFT Food Bank",
            location = "Hardmans Business Centre, New Hall Hey, Rawtenstall, BB4 6HH",
            town = "Rawtenstall",
            description = "Free food parcels for individuals and families in need.",
            types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
            features = listOf("Free food parcels", "Individuals and families", "Emergency support"),
            contact = ContactInfo(phone = "", email = ""),
            schedule = "Tuesday, Thursday, Friday 10:00 – 12:00",
            status = ServiceStatus.APPROVED,
            websiteUrl = "raftfoundation.org"
        ),
        Service(
            id = "positive_start_food_group",
            organizationName = "Positive Start",
            groupName = "Positive Start Food Group",
            location = "4 Bury Road, Rawtenstall, BB4 6AA",
            town = "Rawtenstall",
            description = "Community food distribution and social meet-up.",
            types = listOf(ServiceType.FOOD_BANKS, ServiceType.SOCIAL),
            features = listOf("Food distribution", "Social meet-up", "Community support"),
            contact = ContactInfo(phone = "", email = ""),
            schedule = "Friday 09:30 – 10:30",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "colne_open_door_crisis_dropin",
            organizationName = "Colne Open Door Centre",
            groupName = "Crisis Drop-in & Food Support",
            location = "1 Great George Street, Colne, BB8 0SY",
            town = "Colne",
            description = "Offers food parcels, free counselling, and community café.",
            types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL, ServiceType.MENTAL_HEALTH),
            features = listOf("Food parcels", "Free counselling", "Community café", "Crisis support"),
            contact = ContactInfo(phone = "01282 860342", email = "manager@opendoorcentre.org.uk"),
            schedule = "Monday to Friday 09:00 – 16:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "st_bartholomews_community_grocery",
            organizationName = "St Bartholomew's Church",
            groupName = "Community Grocery",
            location = "Church Street, Colne",
            town = "Colne",
            description = "Affordable food club with refreshments and social interaction.",
            types = listOf(ServiceType.FOOD_BANKS, ServiceType.SOCIAL),
            features = listOf("Affordable food", "Refreshments", "Social interaction", "Food club"),
            contact = ContactInfo(phone = "07547 373970", email = ""),
            schedule = "Friday 09:30 – 11:30",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "west_craven_foodbank",
            organizationName = "West Craven Foodbank",
            groupName = "Emergency Food Support",
            location = "Based in West Craven area (covers parts of Colne)",
            town = "Colne",
            description = "Emergency food support for West Craven area residents.",
            types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
            features = listOf("Emergency food", "West Craven area", "Referral system"),
            contact = ContactInfo(phone = "", email = ""),
            schedule = "By arrangement, contact for details",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "blackburn_food_bank",
            organizationName = "Blackburn Food Bank",
            groupName = "Emergency Food Support",
            location = "Blackburn Community Centre, Blackburn",
            town = "Blackburn",
            description = "Emergency food parcels for individuals and families in need in Blackburn area.",
            types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
            features = listOf("Emergency food", "Blackburn area", "Family support"),
            contact = ContactInfo(phone = "01254 123456", email = "info@blackburnfoodbank.org"),
            schedule = "Monday, Wednesday, Friday 10:00-14:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "accrington_food_bank",
            organizationName = "Accrington Food Bank",
            groupName = "Emergency Food Support",
            location = "Accrington Community Centre, Accrington",
            town = "Accrington",
            description = "Emergency food support for Accrington residents. Referral system available.",
            types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
            features = listOf("Emergency food", "Accrington area", "Referral system"),
            contact = ContactInfo(phone = "01254 654321", email = "info@accringtonfoodbank.org"),
            schedule = "Tuesday, Thursday 09:00-15:00",
            status = ServiceStatus.APPROVED
        ),
        
        // Additional Food Bank Services - 7 new services
        Service(
            id = "church_on_street_cots_bethesda",
            organizationName = "Church on the Street (COTS)",
            groupName = "Food Bank",
            location = "Bethesda Street, Burnley",
            town = "Burnley",
            description = "Food Bank service providing emergency food support to the community.",
            types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
            features = listOf("Emergency food", "Community support", "Food Bank"),
            contact = ContactInfo(phone = "01282 222203", email = ""),
            schedule = "Monday, Tuesday, Wednesday, Thursday, Friday, Sunday (Daily)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.cots-ministries.co.uk"
        ),
        Service(
            id = "clayton_baptist_church_food_bank",
            organizationName = "Clayton Baptist Church",
            groupName = "Food Bank",
            location = "54 Sparth Rd, Clayton-le-Moors, Accrington BB5 5PZ",
            town = "Accrington",
            description = "Food Bank service providing emergency food support to the community.",
            types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
            features = listOf("Emergency food", "Community support", "Food Bank"),
            contact = ContactInfo(phone = "07834724530", email = ""),
            schedule = "Monday, Tuesday, Wednesday, Thursday, Friday (Daily) - After 2pm",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "gannow_community_centre_food_bank",
            organizationName = "Gannow Community Centre",
            groupName = "Food Bank",
            location = "Adamson St, Burnley BB12 6RB",
            town = "Burnley",
            description = "Food Bank service. You can call in to the centre on Adamson Street to collect your parcel but it would be helpful if you phone us first. Pet food is sometimes available.",
            types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
            features = listOf("Emergency food", "Pet food available", "Collection service", "Community support"),
            contact = ContactInfo(phone = "01282 436396", email = "alan.barnes@bprvcs.co.uk"),
            schedule = "Monday, Tuesday, Wednesday, Thursday, Friday (Daily) - 11:00-14:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "burnley_community_kitchen",
            organizationName = "Burnley Community Kitchen",
            groupName = "Burnley Community Kitchen",
            location = "Unit 83, Upper Market Square of Charter Walk Shopping Centre",
            town = "Burnley",
            description = "Food Bank service providing emergency food support to the community.",
            types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
            features = listOf("Emergency food", "Community support", "Food Bank"),
            contact = ContactInfo(phone = "01282 686402", email = "contact@burnleytogether.org.uk"),
            schedule = "Monday, Tuesday, Wednesday, Thursday, Friday (Daily) - 09:00-16:00",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "spacious_places_delivery",
            organizationName = "Spacious Places Delivery",
            groupName = "Spacious Places Delivery",
            location = "Briercliffe Shopping Centre, Briercliffe Road, Burnley BB10 1WB",
            town = "Burnley",
            description = "Food Bank service that can do deliveries to the community.",
            types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
            features = listOf("Emergency food", "Delivery service", "Community support"),
            contact = ContactInfo(phone = "01282 222030", email = "food@spaciousplace.co.uk"),
            schedule = "Monday, Tuesday, Wednesday, Thursday, Friday (Daily)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "maundy_relief_food_bank",
            organizationName = "Maundy Relief",
            groupName = "Maundy Relief",
            location = "29-31 Abbey Street, Accrington, BB5 1EN",
            town = "Accrington",
            description = "Food Bank that supplies food parcels for residents of Accrington. Also provide a community lunch which is a hot meal served 12pm – 1pm Monday to Saturday.",
            types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL, ServiceType.SOCIAL),
            features = listOf("Emergency food", "Community lunch", "Hot meals", "Accrington residents", "Food parcels"),
            contact = ContactInfo(phone = "01254 232328", email = ""),
            schedule = "Monday-Friday (Daily) - 08:00-16:00, Community lunch 12pm-1pm Monday to Saturday",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "nelson_community_mosque_food_bank",
            organizationName = "Burnley & Pendle Food Bank Group",
            groupName = "Nelson Community Mosque Food Bank",
            location = "Burnley, Pendle, Nelson",
            town = "Nelson",
            description = "Food Bank service providing emergency food support to the community.",
            types = listOf(ServiceType.FOOD_BANKS, ServiceType.PRACTICAL),
            features = listOf("Emergency food", "Community support", "Food Bank"),
            contact = ContactInfo(phone = "07873282580", email = ""),
            schedule = "Monday, Tuesday, Wednesday, Thursday, Friday (Daily) - 09:00-17:00",
            status = ServiceStatus.APPROVED
        ),

        // The Nattershack Scheme services - 10 services
        Service(
            id = "the_nattershack_booths_barrowford",
            organizationName = "The Nattershack",
            groupName = "The NatterShack Scheme - Booth's Barrowford",
            location = "Booth's Coffee Shop, Barrowford",
            town = "Barrowford",
            description = "Join us for a brew and a natter. Everyone welcome. No need to book.",
            types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL, ServiceType.MENTAL_HEALTH, ServiceType.COMMUNITY_INTEREST_GROUPS),
            features = listOf("Peer support", "Social activities", "Mental health support", "No booking required"),
            contact = ContactInfo(phone = "The Nattershack", email = "Nattershack@yahoo.com"),
            schedule = "Monday 10:30 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.facebook.com/p/The-Natter-Shack-Scheme-100064392774105/"
        ),
        Service(
            id = "the_nattershack_brierfield_library",
            organizationName = "The Nattershack",
            groupName = "The NatterShack Scheme - Brierfield Library",
            location = "Brierfield Library, Colne Road, Brierfield, Nelson",
            town = "Nelson",
            description = "Join us for a brew and a natter. Everyone welcome. No need to book.",
            types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL, ServiceType.MENTAL_HEALTH, ServiceType.COMMUNITY_INTEREST_GROUPS),
            features = listOf("Peer support", "Social activities", "Mental health support", "No booking required"),
            contact = ContactInfo(phone = "The Nattershack", email = "Nattershack@yahoo.com"),
            schedule = "Monday 14:00-15:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.facebook.com/p/The-Natter-Shack-Scheme-100064392774105/"
        ),
        Service(
            id = "the_nattershack_prairie_sports_village",
            organizationName = "The Nattershack",
            groupName = "The NatterShack Scheme - Prairie Sports Village",
            location = "Prairie Sports Village, Windermere Avenue, Burnley",
            town = "Burnley",
            description = "Join us for a brew and a natter. Everyone welcome. No need to book.",
            types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL, ServiceType.MENTAL_HEALTH, ServiceType.COMMUNITY_INTEREST_GROUPS),
            features = listOf("Peer support", "Social activities", "Mental health support", "No booking required"),
            contact = ContactInfo(phone = "The Nattershack", email = "Nattershack@yahoo.com"),
            schedule = "Tuesday 11:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.facebook.com/p/The-Natter-Shack-Scheme-100064392774105/"
        ),
        Service(
            id = "the_nattershack_dempseys_burnley",
            organizationName = "The Nattershack",
            groupName = "The NatterShack Scheme - Dempsey's Burnley",
            location = "Dempsey's, Briercliffe Road, Burnley",
            town = "Burnley",
            description = "Join us for a brew and a natter. Everyone welcome. No need to book.",
            types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL, ServiceType.MENTAL_HEALTH, ServiceType.COMMUNITY_INTEREST_GROUPS),
            features = listOf("Peer support", "Social activities", "Mental health support", "No booking required"),
            contact = ContactInfo(phone = "The Nattershack", email = "Nattershack@yahoo.com"),
            schedule = "Tuesday 11:00-13:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.facebook.com/p/The-Natter-Shack-Scheme-100064392774105/"
        ),
        Service(
            id = "the_nattershack_swan_and_goose",
            organizationName = "The Nattershack",
            groupName = "The NatterShack Scheme - Swan and Goose",
            location = "The Swan and Goose, Barden Marina, Burnley",
            town = "Burnley",
            description = "Join us for a brew and a natter. Everyone welcome. No need to book.",
            types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL, ServiceType.MENTAL_HEALTH, ServiceType.COMMUNITY_INTEREST_GROUPS),
            features = listOf("Peer support", "Social activities", "Mental health support", "No booking required"),
            contact = ContactInfo(phone = "The Nattershack", email = "Nattershack@yahoo.com"),
            schedule = "Tuesday 14:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.facebook.com/p/The-Natter-Shack-Scheme-100064392774105/"
        ),
        Service(
            id = "the_nattershack_mechanics_theatre_bar",
            organizationName = "The Nattershack",
            groupName = "The NatterShack Scheme - Mechanics Theatre Bar",
            location = "Mechanics Theatre Bar, St James Street, Burnley",
            town = "Burnley",
            description = "Join us for a brew and a natter. Everyone welcome. No need to book.",
            types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL, ServiceType.MENTAL_HEALTH, ServiceType.COMMUNITY_INTEREST_GROUPS),
            features = listOf("Peer support", "Social activities", "Mental health support", "No booking required"),
            contact = ContactInfo(phone = "The Nattershack", email = "Nattershack@yahoo.com"),
            schedule = "Wednesday 11:00-13:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.facebook.com/p/The-Natter-Shack-Scheme-100064392774105/"
        ),
        Service(
            id = "the_nattershack_pendle_heritage_centre",
            organizationName = "The Nattershack",
            groupName = "The NatterShack Scheme - Pendle Heritage Centre",
            location = "Pendle Heritage Centre, Barrowford",
            town = "Barrowford",
            description = "Join us for a brew and a natter. Everyone welcome. No need to book.",
            types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL, ServiceType.MENTAL_HEALTH, ServiceType.COMMUNITY_INTEREST_GROUPS),
            features = listOf("Peer support", "Social activities", "Mental health support", "No booking required"),
            contact = ContactInfo(phone = "The Nattershack", email = "Nattershack@yahoo.com"),
            schedule = "Wednesday 14:00-16:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.facebook.com/p/The-Natter-Shack-Scheme-100064392774105/"
        ),
        Service(
            id = "the_nattershack_kiddy_kids_harle_syke",
            organizationName = "The Nattershack",
            groupName = "The NatterShack Scheme - Kiddy Kids Harle Syke",
            location = "Kiddy Kids, Kingsmill, Queen Street, Harle Syke",
            town = "Burnley",
            description = "Join us for a brew and a natter. Everyone welcome. No need to book.",
            types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL, ServiceType.MENTAL_HEALTH, ServiceType.COMMUNITY_INTEREST_GROUPS),
            features = listOf("Peer support", "Social activities", "Mental health support", "No booking required"),
            contact = ContactInfo(phone = "The Nattershack", email = "Nattershack@yahoo.com"),
            schedule = "Thursday 11:00-13:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.facebook.com/p/The-Natter-Shack-Scheme-100064392774105/"
        ),
        Service(
            id = "the_nattershack_colne_citadel",
            organizationName = "The Nattershack",
            groupName = "The NatterShack Scheme - Colne Citadel",
            location = "Colne Citadel, Market Place, Colne, BB8 0HY",
            town = "Colne",
            description = "Join us for a brew and a natter. Everyone welcome. No need to book.",
            types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL, ServiceType.MENTAL_HEALTH, ServiceType.COMMUNITY_INTEREST_GROUPS),
            features = listOf("Peer support", "Social activities", "Mental health support", "No booking required"),
            contact = ContactInfo(phone = "The Nattershack", email = "Nattershack@yahoo.com"),
            schedule = "Friday 12:00-13:30 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.facebook.com/p/The-Natter-Shack-Scheme-100064392774105/"
        ),
        Service(
            id = "the_nattershack_downtown_coffee_shop",
            organizationName = "The Nattershack",
            groupName = "The NatterShack Scheme - Downtown Coffee Shop Burnley",
            location = "Downtown Coffee Shop above New Look, Burnley",
            town = "Burnley",
            description = "Join us for a brew and a natter. Everyone welcome. No need to book.",
            types = listOf(ServiceType.PEER_SUPPORT, ServiceType.SOCIAL, ServiceType.MENTAL_HEALTH, ServiceType.COMMUNITY_INTEREST_GROUPS),
            features = listOf("Peer support", "Social activities", "Mental health support", "No booking required"),
            contact = ContactInfo(phone = "The Nattershack", email = "Nattershack@yahoo.com"),
            schedule = "Thursday 10:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.facebook.com/p/The-Natter-Shack-Scheme-100064392774105/"
        ),

        // Additional services added 23/04/26 - 31 services
        Service(
            id = "icann_lancashire_benefits_advocacy_service",
            organizationName = "ICANN",
            groupName = "Lancashire Benefits Advocacy Service",
            location = "ICANN, Howick House, Howick Park Avenue, Penwortham, Preston",
            town = "Lancashire Wide",
            description = "Help filling in benefits forms (PIP, ESA50, UC50, Child DLA, AA), preparation for health assessments, assessment support, appeal tribunal support, and help obtaining medical evidence.",
            types = listOf(ServiceType.PRACTICAL),
            features = listOf("Benefits forms support", "Assessment preparation", "Tribunal support", "Medical evidence support"),
            contact = ContactInfo(phone = "01772746061", email = "admin@i-cann.org.uk"),
            schedule = "Monday-Friday 09:00-17:00 (Daily)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://i-cann.net/"
        ),
        Service(
            id = "dwp_household_support_fund",
            organizationName = "Department for Work & Pensions",
            groupName = "Household Support Fund",
            location = "Henry Street, Church, Accrington, BB5 4EP",
            town = "Accrington",
            description = "Funding help with short-term living costs including food, gas and electric bills, water bills, small white goods and essential items, plus budgeting and debt advice.",
            types = listOf(ServiceType.PRACTICAL),
            features = listOf("Living costs support", "Utility bill support", "Essential items support", "Budgeting and debt advice"),
            contact = ContactInfo(phone = "07498536344", email = ""),
            schedule = "Monday-Friday 09:00-17:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.hyndburnleisure.co.uk/"
        ),
        Service(
            id = "shelter_blackburn_support",
            organizationName = "Shelter",
            groupName = "Shelter Blackburn Advice Service",
            location = "Blackburn Central Library, Blackburn, BB2 1AG",
            town = "Blackburn",
            description = "One-to-one personalised support with housing and homelessness issues, emergency helpline support, and free legal advice including court attendance for eviction and housing loss cases.",
            types = listOf(ServiceType.PRACTICAL),
            features = listOf("Housing advice", "Homelessness support", "Emergency helpline", "Legal advice"),
            contact = ContactInfo(phone = "0808 800 4444", email = ""),
            schedule = "Monday-Friday 08:00-17:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://england.shelter.org.uk/"
        ),
        Service(
            id = "community_solutions_purl_in_the_parlour",
            organizationName = "Community Solutions North West",
            groupName = "Purl in the Parlour",
            location = "Elmfield Hall, Gatty Park, Accrington, BB5 4AA",
            town = "Accrington",
            description = "Knit and natter session to learn new knitting skills in a relaxed, friendly atmosphere for beginners and experienced knitters.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL),
            features = listOf("Knitting", "Learning skills", "Social"),
            contact = ContactInfo(phone = "01254 460080", email = ""),
            schedule = "Monday 10:00-12:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "community_solutions_games_lego_group",
            organizationName = "Community Solutions North West",
            groupName = "Games & Lego Group",
            location = "Elmfield Hall, Gatty Park, Accrington, BB5 4AA",
            town = "Accrington",
            description = "Build creative Lego structures, play card games, and enjoy a variety of board games.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL),
            features = listOf("Lego", "Board games", "Card games", "Social"),
            contact = ContactInfo(phone = "01254 460080", email = ""),
            schedule = "Monday 10:00-12:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "community_solutions_scaled_modelling",
            organizationName = "Community Solutions North West",
            groupName = "Scaled Modelling",
            location = "Elmfield Hall, Gatty Park, Accrington, BB5 4AA",
            town = "Accrington",
            description = "Build scaled models, share tips, and enjoy a relaxing, supportive hobby group.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL),
            features = listOf("Model building", "Creative hobby", "Social"),
            contact = ContactInfo(phone = "01254 460080", email = ""),
            schedule = "Monday 13:00-15:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "community_solutions_team_solutions",
            organizationName = "Community Solutions North West",
            groupName = "Team Solutions",
            location = "Elmfield Hall, Gatty Park, Accrington, BB5 4AA",
            town = "Accrington",
            description = "Group session with games and Lego activities in a friendly social environment.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL),
            features = listOf("Lego", "Games", "Social"),
            contact = ContactInfo(phone = "01254 460080", email = ""),
            schedule = "Monday 13:00-15:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "community_solutions_guitar_group",
            organizationName = "Community Solutions North West",
            groupName = "Guitar Group",
            location = "Elmfield Hall, Gatty Park, Accrington, BB5 4AA",
            town = "Accrington",
            description = "Supportive guitar group for everyone from complete beginners to experienced players.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL),
            features = listOf("Guitar", "Music", "Beginner friendly", "Social"),
            contact = ContactInfo(phone = "01254 460080", email = ""),
            schedule = "Tuesday 10:00-12:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "community_solutions_cook_with_us",
            organizationName = "Community Solutions North West",
            groupName = "Cook with Us",
            location = "Elmfield Hall, Gatty Park, Accrington, BB5 4AA",
            town = "Accrington",
            description = "Relaxed, welcoming cooking group for all ages and abilities to prepare simple meals together.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL),
            features = listOf("Cooking", "Inclusive", "Social"),
            contact = ContactInfo(phone = "01254 460080", email = ""),
            schedule = "Tuesday 13:00-15:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "community_solutions_art_group",
            organizationName = "Community Solutions North West",
            groupName = "Art Group",
            location = "Elmfield Hall, Gatty Park, Accrington, BB5 4AA",
            town = "Accrington",
            description = "Painting, drawing, and creative activities in a supportive and social environment.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL),
            features = listOf("Art", "Creativity", "Social"),
            contact = ContactInfo(phone = "01254 460080", email = ""),
            schedule = "Wednesday 10:00-12:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "community_solutions_cooking_workshop",
            organizationName = "Community Solutions North West",
            groupName = "Cooking Workshop",
            location = "Elmfield Hall, Gatty Park, Accrington, BB5 4AA",
            town = "Accrington",
            description = "Free six-week cooking workshop with main courses, desserts, and budget-friendly meal planning.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL),
            features = listOf("Cooking workshop", "Meal planning", "Budget friendly"),
            contact = ContactInfo(phone = "01254 460080", email = ""),
            schedule = "Wednesday 13:00-15:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "community_solutions_grief_loss_cafe",
            organizationName = "Community Solutions North West",
            groupName = "Grief & Loss Cafe",
            location = "Elmfield Hall, Gatty Park, Accrington, BB5 4AA",
            town = "Accrington",
            description = "Supportive group for grief and loss with connection, advice, and a welcoming social space.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL, ServiceType.PEER_SUPPORT),
            features = listOf("Peer support", "Grief support", "Social connection"),
            contact = ContactInfo(phone = "01254 460080", email = ""),
            schedule = "Thursday 10:00-12:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "community_solutions_mens_group",
            organizationName = "Community Solutions North West",
            groupName = "Men's Group",
            location = "Elmfield Hall, Gatty Park, Accrington, BB5 4AA",
            town = "Accrington",
            description = "A social group for men to chat, connect, and build supportive friendships.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL, ServiceType.PEER_SUPPORT),
            features = listOf("Men only", "Peer support", "Social"),
            contact = ContactInfo(phone = "01254 460080", email = ""),
            schedule = "Thursday 10:00-12:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "community_solutions_ladies_group",
            organizationName = "Community Solutions North West",
            groupName = "Ladies Group",
            location = "Elmfield Hall, Gatty Park, Accrington, BB5 4AA",
            town = "Accrington",
            description = "A welcoming social group for women to chat, connect, and make new friends.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL),
            features = listOf("Women only", "Social"),
            contact = ContactInfo(phone = "01254 460080", email = ""),
            schedule = "Thursday 13:00-15:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "community_solutions_open_social",
            organizationName = "Community Solutions North West",
            groupName = "Open Social",
            location = "Elmfield Hall, Gatty Park, Accrington, BB5 4AA",
            town = "Accrington",
            description = "Open social group with pool, music, conversation, and a welcoming atmosphere for everyone.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL),
            features = listOf("Open to all", "Pool", "Music", "Social"),
            contact = ContactInfo(phone = "01254 460080", email = ""),
            schedule = "Thursday 13:00-15:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "community_solutions_sewing_crafts",
            organizationName = "Community Solutions North West",
            groupName = "Sewing Crafts",
            location = "Elmfield Hall, Gatty Park, Accrington, BB5 4AA",
            town = "Accrington",
            description = "Bring your sewing projects, share tips, and learn techniques in a relaxed creative group.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL),
            features = listOf("Sewing", "Crafts", "Creative", "Social"),
            contact = ContactInfo(phone = "01254 460080", email = ""),
            schedule = "Friday 10:00-12:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "community_solutions_shabby_to_chic",
            organizationName = "Community Solutions North West",
            groupName = "Shabby to Chic",
            location = "Elmfield Hall, Gatty Park, Accrington, BB5 4AA",
            town = "Accrington",
            description = "Learn upcycling techniques to transform old items into creative new pieces.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL),
            features = listOf("Upcycling", "Creative", "Social"),
            contact = ContactInfo(phone = "01254 460080", email = ""),
            schedule = "Friday 10:00-12:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "community_solutions_walking_group",
            organizationName = "Community Solutions North West",
            groupName = "Walking Group",
            location = "Elmfield Hall, Gatty Park, Accrington, BB5 4AA",
            town = "Accrington",
            description = "Leisurely local walk for fresh air, gentle activity, and social connection.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS),
            features = listOf("Walking", "Light exercise", "Social"),
            contact = ContactInfo(phone = "01254 460080", email = ""),
            schedule = "Friday 10:30 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "community_solutions_creative_crafts",
            organizationName = "Community Solutions North West",
            groupName = "Creative Crafts",
            location = "Elmfield Hall, Gatty Park, Accrington, BB5 4AA",
            town = "Accrington",
            description = "Craft activities including paper crafts and colouring in a fun social setting.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL),
            features = listOf("Crafts", "Creative activities", "Social"),
            contact = ContactInfo(phone = "01254 460080", email = ""),
            schedule = "Friday 13:00-15:00 (Weekly)",
            status = ServiceStatus.APPROVED
        ),
        Service(
            id = "newground_guided_walks_ladies_only",
            organizationName = "Newground Together",
            groupName = "Guided Walks (Ladies Only)",
            location = "Pendle Women's Forum, 19-21 Market Square, Nelson, BB9 7LP",
            town = "Nelson",
            description = "Health walk for ladies, open to all abilities with graded routes.",
            types = listOf(ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS, ServiceType.COMMUNITY_INTEREST_GROUPS),
            features = listOf("Ladies only", "Guided walk", "All abilities"),
            contact = ContactInfo(phone = "03003305535", email = ""),
            schedule = "Second Monday of every month 09:00-11:00",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.newgroundtogether.co.uk/"
        ),
        Service(
            id = "newground_womens_walk_nelson",
            organizationName = "Newground Together",
            groupName = "Women's Walk",
            location = "Female Friendship Group, Nelson Family Hub, BB9 8EL",
            town = "Nelson",
            description = "Ladies-only health walk lasting up to one hour and thirty minutes.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS),
            features = listOf("Ladies only", "Guided walk", "Weekly"),
            contact = ContactInfo(phone = "03003305535", email = ""),
            schedule = "Monday 09:30-11:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.newgroundtogether.co.uk/"
        ),
        Service(
            id = "newground_guided_walk_ball_grove_colne",
            organizationName = "Newground Together",
            groupName = "Guided Walk - Ball Grove Park Colne",
            location = "Ball Grove Park, Colne, BB8 7HZ",
            town = "Colne",
            description = "Open one-hour walk suitable for all, starting from Ball Grove Park.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS),
            features = listOf("Guided walk", "All abilities", "Social"),
            contact = ContactInfo(phone = "03003305535", email = ""),
            schedule = "Monday 09:30-11:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.newgroundtogether.co.uk/"
        ),
        Service(
            id = "newground_guided_walk_padiham",
            organizationName = "Newground Together",
            groupName = "Guided Walk - Padiham Leisure Centre",
            location = "Padiham Leisure Centre, BB12 8ED",
            town = "Burnley",
            description = "Two guided walks available with different lengths and difficulty options.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS),
            features = listOf("Guided walk", "Multiple route options", "Social"),
            contact = ContactInfo(phone = "03003305535", email = ""),
            schedule = "Monday 13:15-15:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.newgroundtogether.co.uk/"
        ),
        Service(
            id = "newground_rossendale_roamers",
            organizationName = "Newground Together",
            groupName = "Guided Walks - Rossendale Roamers",
            location = "Various locations across the Rossendale Valley",
            town = "Rawtenstall",
            description = "Weekly graded Rossendale Roamers walks at varying locations, usually up to two hours.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS),
            features = listOf("Guided walks", "Graded difficulty", "Weekly"),
            contact = ContactInfo(phone = "03003305535", email = ""),
            schedule = "Tuesday 10:00-12:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.newgroundtogether.co.uk/"
        ),
        Service(
            id = "newground_guided_walk_stubbylee_bacup",
            organizationName = "Newground Together",
            groupName = "Guided Walk in Stubbylee Park",
            location = "Stubbylee Lane, Bacup",
            town = "Bacup",
            description = "Local guided walk around Bacup, grade A, lasting around one hour.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS),
            features = listOf("Guided walk", "Easy route", "Weekly"),
            contact = ContactInfo(phone = "03003305535", email = ""),
            schedule = "Tuesday 13:15-15:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.newgroundtogether.co.uk/"
        ),
        Service(
            id = "newground_guided_walks_east_lancs_wed",
            organizationName = "Newground Together",
            groupName = "Guided Walks - East Lancashire Wednesday",
            location = "To be arranged on the day",
            town = "Burnley",
            description = "Weekly 2-6 mile guided walks at varying East Lancashire locations with grade B and C routes.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS),
            features = listOf("Guided walks", "2-6 miles", "Weekly"),
            contact = ContactInfo(phone = "03003305535", email = ""),
            schedule = "Wednesday 10:00-12:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.newgroundtogether.co.uk/"
        ),
        Service(
            id = "newground_minhaj_womens_walk",
            organizationName = "Newground Together",
            groupName = "Minhaj Women's Walk",
            location = "Hodge House Community Centre, BB9 8LJ",
            town = "Nelson",
            description = "Ladies-only health walk suitable for all abilities.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS),
            features = listOf("Ladies only", "Guided walk", "All abilities"),
            contact = ContactInfo(phone = "03003305535", email = ""),
            schedule = "Wednesday 10:30-11:30 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.newgroundtogether.co.uk/"
        ),
        Service(
            id = "newground_kiddrow_lane_guided_walk",
            organizationName = "Newground Together",
            groupName = "Kiddrow Lane Guided Walk",
            location = "Kiddrow Lane Health Centre, Kiddrow Lane, Burnley, BB12 6LH",
            town = "Burnley",
            description = "Guided walk with class A and B routes, lasting around one hour and thirty minutes.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS),
            features = listOf("Guided walk", "Multiple route classes", "Weekly"),
            contact = ContactInfo(phone = "03003305535", email = ""),
            schedule = "Thursday 11:00-12:30 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.newgroundtogether.co.uk/"
        ),
        Service(
            id = "newground_haslingden_guided_walk",
            organizationName = "Newground Together",
            groupName = "Haslingden Guided Walk",
            location = "Haslingden Community Link, Bury Road, Haslingden, Rossendale, BB4 5PG",
            town = "Haslingden",
            description = "Easy local guided walk from Haslingden Community Link.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS),
            features = listOf("Guided walk", "Easy route", "Weekly"),
            contact = ContactInfo(phone = "03003305535", email = ""),
            schedule = "Thursday 13:30-15:00 (Weekly)",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.newgroundtogether.co.uk/"
        ),
        Service(
            id = "newground_men_children_walk",
            organizationName = "Newground Together",
            groupName = "Men & Children's Walk",
            location = "Burnley Wood Neighbourhood Centre, Burnley, BB11 3NY",
            town = "Burnley",
            description = "Monthly pram-friendly walk for men and children with fathers, grandparents, and carers.",
            types = listOf(ServiceType.COMMUNITY_INTEREST_GROUPS, ServiceType.SOCIAL, ServiceType.SPORT_AND_FITNESS),
            features = listOf("Men and children", "Pram friendly", "Monthly"),
            contact = ContactInfo(phone = "03003305535", email = ""),
            schedule = "Third Saturday of month 10:00-11:30",
            status = ServiceStatus.APPROVED,
            websiteUrl = "https://www.newgroundtogether.co.uk/event/trek-talk-monthly-walk-for-men-and-children/2026-05-16/"
        ),
        
        // Other Services - 1 service
        Service(
            id = "cheeky_monkey",
            organizationName = "Cheeky Monkey",
            groupName = "Cheeky Monkey",
            location = "Various locations",
            town = "Lancashire",
            description = "Placeholder service for testing purposes",
            types = listOf(ServiceType.SOCIAL),
            features = listOf("Placeholder", "Testing"),
            contact = ContactInfo(phone = "", email = ""),
            schedule = "Various times",
            status = ServiceStatus.APPROVED
        )
    ).map { service ->
        service.copy(id = generateServiceId(service.organizationName, service.groupName))
    }

    private const val PREFS_NAME = "service_cache"
    private const val PREFS_KEY = "services_list"
    private val gson = Gson()

    private var listenerRegistration: ListenerRegistration? = null

    init {
        // Enable Firestore offline persistence once
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Stores data locally for offline use
            .build()
        Firebase.firestore.firestoreSettings = settings
    }

    fun loadServicesLive(context: Context, onResult: (List<Service>) -> Unit) {
        val prefs = getPrefs(context)

        // Step 1: Show cached data first
        val cached = loadFromCache(prefs)
        if (cached.isNotEmpty()) {
            onResult(cached)
        } else {
            onResult(hardcodedServices)
        }

        // Step 2: Listen for Firestore changes (works offline too)
        listenerRegistration?.remove()
        listenerRegistration = Firebase.firestore.collection("services")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Log.e("ServiceManager", "Firestore listener error: ${error?.message}")
                    return@addSnapshotListener
                }

                Log.d("ServiceManager", "Received ${snapshot.documents.size} documents from Firestore")
                
                val firestoreServices = snapshot.documents.mapNotNull { doc ->
                    try {
                        val serviceData = doc.data
                        if (serviceData != null) {
                            Log.d("ServiceManager", "Processing document ${doc.id}")
                            Log.d("ServiceManager", "  Raw fields: ${serviceData.keys}")
                            
                            // Create a case-insensitive map for field lookup, removing spaces and slashes
                            val caseInsensitiveData = serviceData.mapKeys { 
                                it.key.toString().lowercase().replace(Regex("[\\s/]"), "")
                            }
                            
                            Log.d("ServiceManager", "  Normalized fields: ${caseInsensitiveData.keys}")
                            
                            val service = Service(
                                id = doc.id,
                                organizationName = getFieldValue(caseInsensitiveData, "organisationname", "organizationname", "organization_name", "organization") ?: "",
                                groupName = getFieldValue(caseInsensitiveData, "groupprogramname", "groupname", "group_name", "group") ?: "",
                                location = getFieldValue(caseInsensitiveData, "locationdetails", "location", "address", "place") ?: "",
                                town = getFieldValue(caseInsensitiveData, "town", "city", "locality", "area") ?: "",
                                description = getFieldValue(caseInsensitiveData, "groupdescription", "description", "desc", "details") ?: "",
                                types = parseServiceTypes(caseInsensitiveData, serviceData),
                                features = parseFeatures(caseInsensitiveData, serviceData),
                                contact = parseContactInfo(caseInsensitiveData, serviceData),
                                schedule = getFieldValue(caseInsensitiveData, "sessiontimes", "schedule", "sessions", "times", "hours") ?: "",
                                status = parseServiceStatus(caseInsensitiveData),
                                isDuplicate = caseInsensitiveData["isduplicate"] as? Boolean ?: false,
                                websiteUrl = getFieldValue(caseInsensitiveData, "websiteurl", "website", "url", "link")
                            )
                            
                            Log.d("ServiceManager", "  Parsed: ${service.organizationName} - ${service.groupName}, status=${service.status}, types=${service.types}")
                            service
                        } else null
                    } catch (e: Exception) {
                        Log.e("ServiceManager", "Error parsing service document ${doc.id}", e)
                        null
                    }
                }
                
                Log.d("ServiceManager", "Successfully parsed ${firestoreServices.size} services from Firestore")
                val updatedList = overrideWithFirestore(firestoreServices, hardcodedServices)
                Log.d("ServiceManager", "Total services after merge with hardcoded: ${updatedList.size}")
                saveToCache(prefs, updatedList)
                onResult(updatedList)
            }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    private fun overrideWithFirestore(firestore: List<Service>, hardcoded: List<Service>): List<Service> {
        val resultMap = hardcoded.associateBy { it.id }.toMutableMap()
        for (service in firestore) {
            resultMap[service.id] = service
        }
        return resultMap.values.toList()
    }

    private fun saveToCache(prefs: SharedPreferences, services: List<Service>) {
        prefs.edit().putString(PREFS_KEY, gson.toJson(services)).apply()
    }

    private fun loadFromCache(prefs: SharedPreferences): List<Service> {
        val json = prefs.getString(PREFS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<Service>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Last merged list persisted under [PREFS_NAME] (JSON), without touching Firestore. */
    fun loadCachedServices(context: Context): List<Service> {
        return loadFromCache(getPrefs(context))
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Utility function to get hardcoded services for admin operations
    fun getHardcodedServices(): List<Service> {
        return hardcodedServices
    }

    // Function to check if Firebase has data
    fun checkFirebaseData(onResult: (Boolean, String) -> Unit) {
        Firebase.firestore.collection("services")
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val hasData = !snapshot.isEmpty
                val message = if (hasData) {
                    "Firebase has ${snapshot.size()} documents"
                } else {
                    "Firebase is empty"
                }
                onResult(hasData, message)
            }
            .addOnFailureListener { e ->
                onResult(false, "Error checking Firebase: ${e.message}")
            }
    }

    // Helper function to get field value with multiple possible field names (case-insensitive)
    private fun getFieldValue(data: Map<String, Any?>, vararg fieldNames: String): String? {
        for (fieldName in fieldNames) {
            data[fieldName.lowercase()]?.let { value ->
                if (value is String && value.isNotBlank()) {
                    return value
                }
            }
        }
        return null
    }

    // Helper function to parse service types with case-insensitive handling
    private fun parseServiceTypes(data: Map<String, Any?>, originalData: Map<String, Any?>): List<ServiceType> {
        // Prefer "Service Type" string — ServiceRepository updates and SubmitServiceScreen write this;
        // a legacy `types` array may be stale if only the string was updated.
        val typesString = data["servicetype"] as? String
        if (!typesString.isNullOrBlank()) {
            return typesString.split(",").mapNotNull { typeName ->
                try {
                    ServiceType.valueOf(typeName.trim().uppercase().replace(" ", "_"))
                } catch (e: IllegalArgumentException) {
                    try {
                        ServiceType.values().find { it.name.equals(typeName.trim(), ignoreCase = true) }
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }

        val rawTypes = data["types"]
        val typesList = when (rawTypes) {
            is List<*> -> rawTypes.mapNotNull { it as? String }
            else -> emptyList()
        }
        if (typesList.isNotEmpty()) {
            return typesList.mapNotNull { typeName ->
                try {
                    ServiceType.valueOf(typeName.trim().uppercase().replace(" ", "_"))
                } catch (e: IllegalArgumentException) {
                    try {
                        ServiceType.values().find { it.name.equals(typeName, ignoreCase = true) }
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }

        return emptyList()
    }

    // Helper function to parse features
    private fun parseFeatures(data: Map<String, Any?>, originalData: Map<String, Any?>): List<String> {
        // First try to get as a list (new format)
        val featuresList = data["features"] as? List<String>
        if (featuresList != null && featuresList.isNotEmpty()) {
            return featuresList
        }
        
        // Try to get as comma-separated string (ServiceRepository format)
        val featuresString = data["features"] as? String
        if (!featuresString.isNullOrBlank()) {
            return featuresString.split(",").map { it.trim() }
        }
        
        return emptyList()
    }

    // Helper function to parse contact info with case-insensitive field names
    private fun parseContactInfo(data: Map<String, Any?>, originalData: Map<String, Any?>): ContactInfo? {
        // First try to get as a nested map (new format)
        val contactData = data["contact"] as? Map<*, *>
        if (contactData != null) {
            val phone = getFieldValue(contactData.mapKeys { it.key.toString().lowercase().replace(Regex("[\\s/]"), "") }, "phone", "telephone", "tel")
            val email = getFieldValue(contactData.mapKeys { it.key.toString().lowercase().replace(Regex("[\\s/]"), "") }, "email", "mail")
            if (!phone.isNullOrBlank() || !email.isNullOrBlank()) {
                return ContactInfo(
                    phone = phone ?: "",
                    email = email ?: ""
                )
            }
        }
        
        // Try to get as a simple string (ServiceRepository format - "Contact Information")
        val contactString = data["contactinformation"] as? String
        if (!contactString.isNullOrBlank()) {
            return ContactInfo(
                phone = contactString,
                email = ""
            )
        }
        
        return null
    }

    // Helper function to parse service status with case-insensitive handling
    private fun parseServiceStatus(data: Map<String, Any?>): ServiceStatus {
        val statusString = getFieldValue(data, "status", "state")
        return try {
            ServiceStatus.valueOf(statusString?.uppercase() ?: "PENDING")
        } catch (e: IllegalArgumentException) {
            try {
                // Try case-insensitive match
                ServiceStatus.values().find { it.name.equals(statusString, ignoreCase = true) } ?: ServiceStatus.PENDING
            } catch (e: Exception) {
                ServiceStatus.PENDING
            }
        }
    }
}
