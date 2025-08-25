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
    )

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
                if (error != null || snapshot == null) return@addSnapshotListener

                val firestoreServices = snapshot.documents.mapNotNull { doc ->
                    try {
                        val serviceData = doc.data
                        if (serviceData != null) {
                            // Create a case-insensitive map for field lookup
                            val caseInsensitiveData = serviceData.mapKeys { it.key.toString().lowercase() }
                            
                            Service(
                                id = doc.id,
                                organizationName = getFieldValue(caseInsensitiveData, "organizationname", "organization_name", "organization") ?: "",
                                groupName = getFieldValue(caseInsensitiveData, "groupname", "group_name", "group") ?: "",
                                location = getFieldValue(caseInsensitiveData, "location", "address", "place") ?: "",
                                description = getFieldValue(caseInsensitiveData, "description", "desc", "details") ?: "",
                                types = parseServiceTypes(caseInsensitiveData),
                                features = parseFeatures(caseInsensitiveData),
                                contact = parseContactInfo(caseInsensitiveData),
                                schedule = getFieldValue(caseInsensitiveData, "schedule", "sessions", "times", "hours"),
                                status = parseServiceStatus(caseInsensitiveData),
                                isDuplicate = caseInsensitiveData["isduplicate"] as? Boolean ?: false,
                                websiteUrl = getFieldValue(caseInsensitiveData, "websiteurl", "website", "url", "link")
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
                
                val updatedList = overrideWithFirestore(firestoreServices, hardcodedServices)
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
    private fun parseServiceTypes(data: Map<String, Any?>): List<ServiceType> {
        val typesList = data["types"] as? List<String> ?: emptyList()
        return typesList.mapNotNull { typeName ->
            try {
                // Try exact match first
                ServiceType.valueOf(typeName.uppercase())
            } catch (e: IllegalArgumentException) {
                try {
                    // Try case-insensitive match
                    ServiceType.values().find { it.name.equals(typeName, ignoreCase = true) }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    // Helper function to parse features
    private fun parseFeatures(data: Map<String, Any?>): List<String> {
        return data["features"] as? List<String> ?: emptyList()
    }

    // Helper function to parse contact info with case-insensitive field names
    private fun parseContactInfo(data: Map<String, Any?>): ContactInfo? {
        val contactData = data["contact"] as? Map<*, *>
        if (contactData != null) {
            val phone = getFieldValue(contactData.mapKeys { it.key.toString().lowercase() }, "phone", "telephone", "tel")
            val email = getFieldValue(contactData.mapKeys { it.key.toString().lowercase() }, "email", "mail")
            if (!phone.isNullOrBlank() || !email.isNullOrBlank()) {
                return ContactInfo(
                    phone = phone ?: "",
                    email = email ?: ""
                )
            }
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
