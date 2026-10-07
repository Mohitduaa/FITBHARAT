import SwiftUI
import WidgetKit

// MARK: - Data written by the app (see IosWidgetBridge.kt)

struct SnapshotRow: Codable {
    let label: String
    let value: String
    let progress: Double
}

struct Snapshot: Codable {
    let light: Bool
    let ringValue: String
    let ringLabel: String
    let ringProgress: Double
    let over: Bool
    let rows: [SnapshotRow]
    let updatedAtMillis: Double

    static let sample = Snapshot(
        light: false, ringValue: "845", ringLabel: "kcal left", ringProgress: 0.58, over: false,
        rows: [
            SnapshotRow(label: "Steps", value: "5,240 / 7,500", progress: 0.70),
            SnapshotRow(label: "Water", value: "1.8 / 3.0 L", progress: 0.60),
            SnapshotRow(label: "Protein", value: "68 / 120 g", progress: 0.57),
        ],
        updatedAtMillis: 0
    )
}

/// AltStore may register the App Group under another id and lists the real one in ALTAppGroups.
private func appGroupId() -> String {
    if let groups = Bundle.main.object(forInfoDictionaryKey: "ALTAppGroups") as? [String], let first = groups.first {
        return first
    }
    return "group.com.fitbharat.app"
}

private func loadSnapshot() -> Snapshot? {
    guard let defaults = UserDefaults(suiteName: appGroupId()),
          let json = defaults.string(forKey: "fb_widget_snapshot"),
          let data = json.data(using: .utf8) else { return nil }
    return try? JSONDecoder().decode(Snapshot.self, from: data)
}

// MARK: - Timeline

struct FitBharatEntry: TimelineEntry {
    let date: Date
    let snapshot: Snapshot?
}

struct FitBharatProvider: TimelineProvider {
    func placeholder(in context: Context) -> FitBharatEntry {
        FitBharatEntry(date: Date(), snapshot: .sample)
    }

    func getSnapshot(in context: Context, completion: @escaping (FitBharatEntry) -> Void) {
        completion(FitBharatEntry(date: Date(), snapshot: loadSnapshot() ?? .sample))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<FitBharatEntry>) -> Void) {
        let entry = FitBharatEntry(date: Date(), snapshot: loadSnapshot())
        // The app also asks for a redraw when it goes to the background.
        completion(Timeline(entries: [entry], policy: .after(Date().addingTimeInterval(15 * 60))))
    }
}

// MARK: - Look (same palette as the Android widget)

private extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}

private struct Palette {
    let background: [Color]
    let text: Color
    let secondary: Color
    let track: Color
    let bars: [Color]

    static let dark = Palette(
        background: [Color(hex: 0x38342E), Color(hex: 0x1B1A17)],
        text: Color(hex: 0xF5F1EA), secondary: Color(hex: 0xB1ACA0), track: Color(hex: 0x45413A),
        bars: [Color(hex: 0xEE8A47), Color(hex: 0x5A9FD4), Color(hex: 0x9C8BD6)]
    )
    static let light = Palette(
        background: [Color(hex: 0xFFFFFF), Color(hex: 0xF6F0E6)],
        text: Color(hex: 0x1F1E1B), secondary: Color(hex: 0x6B6860), track: Color(hex: 0xECE6DC),
        bars: [Color(hex: 0xD9622B), Color(hex: 0x3F7CA8), Color(hex: 0x7E6BC4)]
    )
}

private struct CalorieRing: View {
    let snapshot: Snapshot
    let palette: Palette

    var body: some View {
        ZStack {
            Circle().stroke(palette.track, lineWidth: 9)
            Circle()
                .trim(from: 0, to: max(0.02, min(1, snapshot.ringProgress)))
                .stroke(snapshot.over ? Color(hex: 0xE5655B) : palette.bars[0],
                        style: StrokeStyle(lineWidth: 9, lineCap: .round))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 0) {
                Text(snapshot.ringValue)
                    .font(.system(size: 22, weight: .bold, design: .rounded))
                    .foregroundColor(palette.text)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                Text(snapshot.ringLabel)
                    .font(.system(size: 10))
                    .foregroundColor(palette.secondary)
            }
            .padding(.horizontal, 10)
        }
    }
}

private struct ProgressRow: View {
    let row: SnapshotRow
    let color: Color
    let palette: Palette

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(row.label).font(.system(size: 11)).foregroundColor(palette.secondary)
                Spacer(minLength: 4)
                Text(row.value).font(.system(size: 11, weight: .bold)).foregroundColor(palette.text).lineLimit(1)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(palette.track)
                    Capsule().fill(color).frame(width: geo.size.width * max(0, min(1, row.progress)))
                }
            }
            .frame(height: 6)
        }
    }
}

struct FitBharatWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: FitBharatEntry

    var body: some View {
        let snapshot = entry.snapshot
        let palette = (snapshot?.light ?? false) ? Palette.light : Palette.dark
        content(snapshot, palette)
            .widgetBackground(
                LinearGradient(colors: palette.background, startPoint: .topLeading, endPoint: .bottomTrailing)
            )
    }

    @ViewBuilder
    private func content(_ snapshot: Snapshot?, _ palette: Palette) -> some View {
        if let snapshot {
            if family == .systemSmall {
                CalorieRing(snapshot: snapshot, palette: palette).padding(6)
            } else {
                HStack(spacing: 16) {
                    CalorieRing(snapshot: snapshot, palette: palette).frame(width: 100, height: 100)
                    VStack(spacing: 10) {
                        ForEach(Array(snapshot.rows.prefix(3).enumerated()), id: \.offset) { index, row in
                            ProgressRow(row: row, color: palette.bars[index % palette.bars.count], palette: palette)
                        }
                    }
                }
            }
        } else {
            VStack(spacing: 4) {
                Text("FitBharat").font(.system(size: 14, weight: .bold)).foregroundColor(palette.bars[0])
                Text("Open the app once to show today's progress here.")
                    .font(.system(size: 11)).foregroundColor(palette.secondary).multilineTextAlignment(.center)
            }
        }
    }
}

private extension View {
    /// iOS 17 requires containerBackground for widgets; older versions use a plain background.
    @ViewBuilder
    func widgetBackground<Background: View>(_ background: Background) -> some View {
        if #available(iOSApplicationExtension 17.0, *) {
            containerBackground(for: .widget) { background }
        } else {
            self.padding().background(background)
        }
    }
}

@main
struct FitBharatWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "FitBharatWidget", provider: FitBharatProvider()) { entry in
            FitBharatWidgetView(entry: entry)
        }
        .configurationDisplayName("FitBharat")
        .description("Calories left and today's progress.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
