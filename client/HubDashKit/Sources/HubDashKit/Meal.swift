import Foundation

public enum MealType: String, Codable, CaseIterable, Sendable {
    case breakfast = "BREAKFAST"
    case lunch = "LUNCH"
    case dinner = "DINNER"
    case snack = "SNACK"

    public var koreanName: String {
        switch self {
        case .breakfast: return "아침"
        case .lunch: return "점심"
        case .dinner: return "저녁"
        case .snack: return "간식"
        }
    }
}

/// 음식 항목들의 영양 합계. 값을 비워 둔 항목은 서버가 0으로 취급한다.
public struct MealTotals: Codable, Equatable, Sendable {
    public let calories: Int
    public let carbsG: Double
    public let proteinG: Double
    public let fatG: Double
    public let sodiumMg: Double
}

/// 끼니에 추가한 음식 한 개.
public struct MealItem: Codable, Equatable, Identifiable, Sendable {
    public let id: Int
    public let mealRecordId: Int
    public let name: String
    public let calories: Int
    public let carbsG: Double?
    public let proteinG: Double?
    public let fatG: Double?
    public let sodiumMg: Double?
    public let createdAt: Date
    public let updatedAt: Date
}

/// 한 끼(아침/점심/저녁/간식). 영양 합계는 음식 항목의 합으로 서버가 계산해 준다.
public struct MealRecord: Codable, Equatable, Identifiable, Sendable {
    public let id: Int
    public let consumedAt: Date
    public let mealType: MealType
    public let notes: String?
    public let items: [MealItem]
    public let totals: MealTotals
    public let createdAt: Date
    public let updatedAt: Date
}

public struct MealRecordRequest: Encodable, Sendable {
    public var consumedAt: Date
    public var mealType: MealType
    public var notes: String?

    public init(consumedAt: Date, mealType: MealType, notes: String? = nil) {
        self.consumedAt = consumedAt
        self.mealType = mealType
        self.notes = notes
    }
}

/// 칼로리는 필수, 탄단지/나트륨은 모르면 비워 둘 수 있다(nil은 JSON에서 생략된다).
public struct MealItemRequest: Encodable, Sendable {
    public var mealRecordId: Int
    public var name: String
    public var calories: Int
    public var carbsG: Double?
    public var proteinG: Double?
    public var fatG: Double?
    public var sodiumMg: Double?

    public init(mealRecordId: Int, name: String, calories: Int,
                carbsG: Double? = nil, proteinG: Double? = nil, fatG: Double? = nil, sodiumMg: Double? = nil) {
        self.mealRecordId = mealRecordId
        self.name = name
        self.calories = calories
        self.carbsG = carbsG
        self.proteinG = proteinG
        self.fatG = fatG
        self.sodiumMg = sodiumMg
    }
}

/// 하루 총합 + 끼니 종류별 합계. 기록이 없는 끼니도 0으로 채워져 항상 4개가 온다.
public struct DailyMealSummary: Codable, Equatable, Sendable {
    public struct MealTypeSummary: Codable, Equatable, Sendable {
        public let mealType: MealType
        public let itemCount: Int
        public let totals: MealTotals
    }

    public let date: LocalDate
    public let totals: MealTotals
    public let meals: [MealTypeSummary]
}
