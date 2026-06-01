import SwiftUI

@Observable
final class MissionListVm {
    var items: [MissionDto] = []
    var error: String? = nil
    private let repo = MissionsRepository.shared
    func refresh() async { items = (try? await repo.list()) ?? [] }
    func delete(_ id: String) async {
        do { try await repo.delete(id) }
        catch { self.error = (error as? LocalizedError)?.errorDescription }
        await refresh()
    }
}

struct MissionListView: View {
    @State private var vm = MissionListVm()
    @State private var editing: MissionDto? = nil
    @State private var creating = false

    var body: some View {
        WarmBackground {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    if let error = vm.error { ErrorText(text: error) }
                    if vm.items.isEmpty {
                        EmptyState(text: "미션을 추가해보세요")
                    } else {
                        ForEach(vm.items) { m in
                            ListRow(
                                headline: m.name,
                                supporting: m.type.label + (m.isDefault ? " · 기본" : ""),
                                onTap: { editing = m },
                                trailing: {
                                    Button("삭제") { Task { await vm.delete(m.id) } }
                                        .foregroundStyle(FA.destructive)
                                        .font(.system(size: 14))
                                }
                            )
                        }
                    }
                    Spacer().frame(height: FASpacing.xxl)
                }
                .padding(.horizontal, FASpacing.lg)
            }
        }
        .navigationTitle("미션")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("추가") { creating = true }
            }
        }
        .task { await vm.refresh() }
        .sheet(isPresented: $creating, onDismiss: { Task { await vm.refresh() } }) {
            MissionEditView(mission: nil)
        }
        .sheet(item: $editing, onDismiss: { Task { await vm.refresh() } }) { m in
            MissionEditView(mission: m)
        }
    }
}

@Observable
final class MissionEditVm {
    var type: MissionType = .MATH
    var name: String = ""
    var difficulty: String = "medium"
    var questionCount: Double = 3
    var photoMode: String = "QR"
    var expectedCode: String = ""
    var shakeCount: Double = 30
    var isDefault: Bool = false
    var saving = false
    var saved = false
    var error: String? = nil
    private let repo = MissionsRepository.shared
    private var editingId: String? = nil

    func load(_ m: MissionDto?) {
        guard let m else { return }
        editingId = m.id
        type = m.type
        name = m.name
        isDefault = m.isDefault
        let cfg = m.config.raw
        difficulty = cfg["difficulty"]?.stringValue ?? difficulty
        questionCount = Double(cfg["questionCount"]?.intValue ?? Int(questionCount))
        photoMode = cfg["mode"]?.stringValue ?? photoMode
        expectedCode = cfg["expectedCode"]?.stringValue ?? ""
        shakeCount = Double(cfg["shakeCount"]?.intValue ?? Int(shakeCount))
    }

    func save() async {
        var config: [String: AnyCodable] = [:]
        switch type {
        case .MATH:
            config["difficulty"] = .string(difficulty)
            config["questionCount"] = .int(Int(questionCount))
        case .PHOTO:
            config["mode"] = .string(photoMode)
            if !expectedCode.isEmpty {
                config["expectedCode"] = .string(expectedCode.trimmingCharacters(in: .whitespaces))
            }
        case .SHAKE:
            config["shakeCount"] = .int(Int(shakeCount))
        }
        saving = true; error = nil
        let cfg = MissionConfig(config)
        let realName = name.isEmpty ? type.rawValue : name
        do {
            if let id = editingId {
                _ = try await repo.update(id, UpdateMissionRequest(
                    name: name.isEmpty ? nil : name, config: cfg, isDefault: isDefault
                ))
            } else {
                _ = try await repo.create(CreateMissionRequest(
                    type: type, name: realName, config: cfg, isDefault: isDefault
                ))
            }
            saved = true
        } catch {
            self.error = (error as? LocalizedError)?.errorDescription
        }
        saving = false
    }
}

struct MissionEditView: View {
    @Environment(\.dismiss) private var dismiss
    let mission: MissionDto?
    @State private var vm = MissionEditVm()

    var body: some View {
        WarmBackground {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Button("취소") { dismiss() }
                        .foregroundStyle(FA.primary)
                    Spacer()
                    Text(mission == nil ? "새 미션" : "미션 수정")
                        .font(.system(size: 17, weight: .semibold))
                    Spacer()
                    Button("저장") {
                        Task {
                            await vm.save()
                            if vm.saved { dismiss() }
                        }
                    }
                    .foregroundStyle(FA.primary)
                    .disabled(vm.saving)
                }
                .padding(.horizontal, FASpacing.lg)
                .padding(.vertical, FASpacing.md)

                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        FASection(title: "이름") {
                            FaTextField(placeholder: "미션 이름", text: $vm.name)
                        }

                        FASection(title: "종류") {
                            HStack(spacing: FASpacing.sm) {
                                ForEach(MissionType.allCases, id: \.self) { t in
                                    ChoicePill(
                                        text: t.label,
                                        selected: vm.type == t,
                                        action: { vm.type = t }
                                    )
                                }
                            }
                        }

                        switch vm.type {
                        case .MATH:
                            FASection(title: "난이도") {
                                HStack(spacing: FASpacing.sm) {
                                    ForEach([("easy","쉬움"), ("medium","보통"), ("hard","어려움")], id: \.0) { v, l in
                                        ChoicePill(
                                            text: l,
                                            selected: vm.difficulty == v,
                                            action: { vm.difficulty = v }
                                        )
                                    }
                                }
                            }
                            FASection(title: "문제 수") {
                                Text("\(Int(vm.questionCount))문제")
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundStyle(FA.labelSecondary)
                                Slider(value: $vm.questionCount, in: 1...10, step: 1)
                                    .tint(FA.primary)
                            }
                        case .PHOTO:
                            FASection(title: "모드") {
                                HStack(spacing: FASpacing.sm) {
                                    ForEach([("QR","QR"), ("BARCODE","바코드"), ("REFERENCE_IMAGE","기준 사진")], id: \.0) { v, l in
                                        ChoicePill(
                                            text: l,
                                            selected: vm.photoMode == v,
                                            action: { vm.photoMode = v }
                                        )
                                    }
                                }
                            }
                            if vm.photoMode == "QR" || vm.photoMode == "BARCODE" {
                                FASection(title: "기대 코드 (선택)") {
                                    FaTextField(placeholder: "비우면 어떤 코드든 통과", text: $vm.expectedCode)
                                }
                            } else {
                                FASection(title: "기준 사진") {
                                    Text(vm.expectedCode.isEmpty
                                         ? "기준 사진 등록 기능은 향후 추가됩니다"
                                         : "등록됨 (해시 \(vm.expectedCode.prefix(8))…)")
                                        .font(.system(size: 14))
                                        .foregroundStyle(FA.labelSecondary)
                                }
                            }
                        case .SHAKE:
                            FASection(title: "흔들기 횟수") {
                                Text("\(Int(vm.shakeCount))회")
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundStyle(FA.labelSecondary)
                                Slider(value: $vm.shakeCount, in: 5...100, step: 1)
                                    .tint(FA.primary)
                            }
                        }

                        FASection(title: "옵션") {
                            ListRow(
                                headline: "기본 미션으로 설정",
                                supporting: "새 알람 만들 때 자동으로 선택돼요",
                                trailing: {
                                    Toggle("", isOn: $vm.isDefault)
                                        .labelsHidden()
                                        .tint(FA.primary)
                                }
                            )
                        }

                        if let error = vm.error { ErrorText(text: error) }
                        Spacer().frame(height: FASpacing.xxl)
                    }
                    .padding(.horizontal, FASpacing.lg)
                }
            }
        }
        .task { vm.load(mission) }
    }
}
