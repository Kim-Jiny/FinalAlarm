import SwiftUI
import CoreMotion
import AVFoundation
import Vision

// MARK: - 미션 러너 통합 진입점

struct MissionRunnerView: View {
    let mission: MissionDto
    let onSuccess: () -> Void
    let onCancel: () -> Void

    var body: some View {
        switch mission.type {
        case .MATH:
            MathRunnerView(mission: mission, onSuccess: onSuccess, onCancel: onCancel)
        case .PHOTO:
            BarcodeRunnerView(mission: mission, onSuccess: onSuccess, onCancel: onCancel)
        case .SHAKE:
            ShakeRunnerView(mission: mission, onSuccess: onSuccess, onCancel: onCancel)
        }
    }
}

// MARK: - 수학

struct MathRunnerView: View {
    let mission: MissionDto
    let onSuccess: () -> Void
    let onCancel: () -> Void

    @State private var problems: [(String, Int)] = []
    @State private var idx: Int = 0
    @State private var input: String = ""
    @State private var error: String? = nil

    var body: some View {
        WarmBackground {
            VStack(alignment: .leading, spacing: FASpacing.lg) {
                HStack {
                    Button("취소", action: onCancel)
                        .foregroundStyle(FA.primary)
                    Spacer()
                    Text("\(idx + 1) / \(problems.count)")
                        .font(.system(size: 14))
                        .foregroundStyle(FA.labelSecondary)
                }
                .padding(.horizontal, FASpacing.lg)
                .padding(.top, FASpacing.sm)

                Spacer()
                if !problems.isEmpty {
                    Text(problems[idx].0)
                        .font(.system(size: 64, weight: .heavy))
                        .foregroundStyle(FA.label)
                        .frame(maxWidth: .infinity)

                    FaTextField(placeholder: "정답", text: $input, keyboardType: .numberPad)
                        .padding(.horizontal, FASpacing.lg)

                    if let error { ErrorText(text: error).padding(.horizontal, FASpacing.lg) }
                }
                Spacer()
                PrimaryButton(text: idx + 1 == problems.count ? "완료" : "다음", enabled: !input.isEmpty) {
                    submit()
                }
                .padding(.horizontal, FASpacing.lg)
                Spacer().frame(height: FASpacing.md)
            }
        }
        .onAppear { problems = generate() }
    }

    private func generate() -> [(String, Int)] {
        let cfg = mission.config.raw
        let difficulty = cfg["difficulty"]?.stringValue ?? "medium"
        let count = cfg["questionCount"]?.intValue ?? 3
        let range: ClosedRange<Int> = switch difficulty {
        case "easy": 1...20
        case "hard": 10...99
        default: 5...50
        }
        return (0..<count).map { _ in
            let a = Int.random(in: range)
            let b = Int.random(in: range)
            let op = ["+", "-", "×"].randomElement()!
            let answer: Int = switch op {
            case "+": a + b
            case "-": a - b
            default: a * b
            }
            return ("\(a) \(op) \(b)", answer)
        }
    }

    private func submit() {
        guard !problems.isEmpty else { return }
        guard let entered = Int(input.trimmingCharacters(in: .whitespaces)),
              entered == problems[idx].1 else {
            error = "다시 풀어봐요"
            input = ""
            return
        }
        error = nil
        input = ""
        if idx + 1 < problems.count {
            idx += 1
        } else {
            onSuccess()
        }
    }
}

// MARK: - 사진 (QR / BARCODE via Vision)

struct BarcodeRunnerView: View {
    let mission: MissionDto
    let onSuccess: () -> Void
    let onCancel: () -> Void

    @State private var lastError: String? = nil

    var body: some View {
        ZStack {
            CameraScannerView(
                onDetect: { code in
                    let expected = mission.config.raw["expectedCode"]?.stringValue?.trimmingCharacters(in: .whitespaces)
                    if let expected, !expected.isEmpty, code != expected {
                        lastError = "코드가 일치하지 않아요"
                        return false
                    }
                    onSuccess()
                    return true
                }
            )
            .ignoresSafeArea()

            VStack {
                HStack {
                    Button(action: onCancel) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 28))
                            .foregroundStyle(.white.opacity(0.9))
                    }
                    Spacer()
                }
                .padding(.horizontal, FASpacing.lg)
                .padding(.top, FASpacing.lg)
                Spacer()
                VStack(spacing: FASpacing.sm) {
                    Text("QR / 바코드를 카메라에 비추세요")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(.white)
                    if let lastError {
                        Text(lastError)
                            .font(.system(size: 14))
                            .foregroundStyle(FA.destructive)
                    }
                }
                .padding()
                .background(Color.black.opacity(0.45))
                .clipShape(RoundedRectangle(cornerRadius: FACorner.medium))
                .padding(.bottom, FASpacing.xxxl)
            }
        }
    }
}

// CameraX의 iOS 대응 — AVCaptureSession + Vision (VNDetectBarcodesRequest)
struct CameraScannerView: UIViewControllerRepresentable {
    let onDetect: (String) -> Bool

    func makeUIViewController(context: Context) -> ScannerVC {
        let vc = ScannerVC()
        vc.onDetect = onDetect
        return vc
    }
    func updateUIViewController(_ uiViewController: ScannerVC, context: Context) {}
}

final class ScannerVC: UIViewController, AVCaptureVideoDataOutputSampleBufferDelegate {
    var onDetect: ((String) -> Bool)?
    private let session = AVCaptureSession()
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private var lastFire: Date = .distantPast

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        configureSession()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.frame = view.bounds
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if !session.isRunning {
            DispatchQueue.global(qos: .userInitiated).async { [weak self] in
                self?.session.startRunning()
            }
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if session.isRunning { session.stopRunning() }
    }

    private func configureSession() {
        session.beginConfiguration()
        session.sessionPreset = .high
        guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
              let input = try? AVCaptureDeviceInput(device: device) else {
            session.commitConfiguration()
            return
        }
        if session.canAddInput(input) { session.addInput(input) }

        let output = AVCaptureVideoDataOutput()
        output.setSampleBufferDelegate(self, queue: DispatchQueue(label: "fa.scanner"))
        if session.canAddOutput(output) { session.addOutput(output) }
        session.commitConfiguration()

        let preview = AVCaptureVideoPreviewLayer(session: session)
        preview.videoGravity = .resizeAspectFill
        preview.frame = view.bounds
        view.layer.addSublayer(preview)
        previewLayer = preview
    }

    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        guard Date().timeIntervalSince(lastFire) > 0.5,
              let pb = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        let handler = VNImageRequestHandler(cvPixelBuffer: pb, orientation: .right)
        let req = VNDetectBarcodesRequest { [weak self] r, _ in
            guard let results = r.results as? [VNBarcodeObservation],
                  let first = results.first,
                  let payload = first.payloadStringValue,
                  let self else { return }
            self.lastFire = Date()
            DispatchQueue.main.async {
                _ = self.onDetect?(payload)
            }
        }
        try? handler.perform([req])
    }
}

// MARK: - 흔들기

struct ShakeRunnerView: View {
    let mission: MissionDto
    let onSuccess: () -> Void
    let onCancel: () -> Void

    @State private var count: Int = 0
    @State private var target: Int = 30
    private let motion = CMMotionManager()

    var body: some View {
        WarmBackground {
            VStack(alignment: .center, spacing: FASpacing.lg) {
                HStack {
                    Button("취소", action: onCancel).foregroundStyle(FA.primary)
                    Spacer()
                }
                .padding(.horizontal, FASpacing.lg)
                .padding(.top, FASpacing.sm)

                Spacer()
                Text("📱")
                    .font(.system(size: 80))
                Text("핸드폰을 흔들어요")
                    .font(.system(size: 22, weight: .heavy))
                    .foregroundStyle(FA.label)
                Text("\(count) / \(target)")
                    .font(.system(size: 48, weight: .heavy))
                    .foregroundStyle(FA.primary)
                Spacer()
            }
        }
        .onAppear { startMotion() }
        .onDisappear { motion.stopAccelerometerUpdates() }
    }

    private func startMotion() {
        target = mission.config.raw["shakeCount"]?.intValue ?? 30
        guard motion.isAccelerometerAvailable else {
            onSuccess()
            return
        }
        motion.accelerometerUpdateInterval = 0.05
        var lastShake = Date.distantPast
        motion.startAccelerometerUpdates(to: .main) { data, _ in
            guard let d = data else { return }
            let mag = sqrt(d.acceleration.x * d.acceleration.x +
                           d.acceleration.y * d.acceleration.y +
                           d.acceleration.z * d.acceleration.z)
            // 1g 정상 + 강한 흔들기 1.5g 이상
            if mag > 2.2, Date().timeIntervalSince(lastShake) > 0.25 {
                lastShake = Date()
                count += 1
                if count >= target {
                    motion.stopAccelerometerUpdates()
                    onSuccess()
                }
            }
        }
    }
}
