import Foundation

/// 식단(끼니/음식 항목/하루 합계) API.
extension APIClient {
    public func createMeal(_ request: MealRecordRequest) async throws -> MealRecord {
        try await post("/api/health/meal-records", body: request)
    }

    public func meal(id: Int) async throws -> MealRecord {
        try await get("/api/health/meal-records/\(id)")
    }

    public func meals(page: Int = 0, size: Int = 20) async throws -> Page<MealRecord> {
        try await get("/api/health/meal-records", query: [
            URLQueryItem(name: "page", value: String(page)),
            URLQueryItem(name: "size", value: String(size)),
        ])
    }

    public func updateMeal(id: Int, _ request: MealRecordRequest) async throws -> MealRecord {
        try await put("/api/health/meal-records/\(id)", body: request)
    }

    public func deleteMeal(id: Int) async throws {
        try await delete("/api/health/meal-records/\(id)")
    }

    public func addItem(_ request: MealItemRequest) async throws -> MealItem {
        try await post("/api/health/meal-items", body: request)
    }

    /// 항목을 수정한다. `mealRecordId`를 바꾸면 다른 끼니로 옮길 수 있다.
    public func updateItem(id: Int, _ request: MealItemRequest) async throws -> MealItem {
        try await put("/api/health/meal-items/\(id)", body: request)
    }

    public func deleteItem(id: Int) async throws {
        try await delete("/api/health/meal-items/\(id)")
    }

    public func dailySummary(date: LocalDate) async throws -> DailyMealSummary {
        try await get("/api/health/meal-records/daily-summary", query: [URLQueryItem(name: "date", value: date.description)])
    }
}
