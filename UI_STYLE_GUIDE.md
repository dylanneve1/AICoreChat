# AICore Chat UI Style Guide

## Design Principles
- **Material 3 First**: Follow Material 3 design guidelines consistently
- **Clarity**: Clear visual hierarchy with proper spacing and typography
- **Consistency**: Use common components for similar UI patterns
- **Accessibility**: Ensure proper contrast ratios and touch targets
- **Performance**: Minimize recompositions with proper state management

## Color System
Use MaterialTheme colors exclusively:
- **Primary**: Brand colors for key actions
- **Surface**: Background colors with proper elevation
- **Container colors**: Use with alpha for subtle backgrounds
  - Primary container: `MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)`
  - Surface variant: `MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)`
  - Error container: `MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)`

## Typography
- **Headlines**: Use for screen titles
  - `headlineLarge`: Main app title
  - `headlineSmall`: Empty states
- **Titles**: Use for sections and cards
  - `titleLarge`: Screen titles in TopAppBar
  - `titleMedium`: Card titles
  - `titleSmall`: Section headers, subsections
- **Body**: Use for content
  - `bodyLarge`: Primary content
  - `bodyMedium`: Secondary content, descriptions
  - `bodySmall`: Supporting text, captions

## Spacing
Standard spacing tokens (in dp):
- 4: Minimal spacing
- 8: Small spacing
- 12: Component internal spacing
- 16: Standard spacing
- 20: Card padding
- 24: Section spacing
- 32: Large spacing

## Common Components

### Cards
```kotlin
// Navigation card
SettingsNavigationCard(
    icon = Icons.Outlined.Settings,
    title = "Title",
    description = "Description",
    onClick = { }
)

// Feature toggle
FeatureToggleCard(
    icon = Icons.Outlined.Feature,
    title = "Feature Name",
    description = "Feature description",
    checked = state,
    onCheckedChange = { }
)

// Info/Warning card
InfoCard(
    icon = Icons.Outlined.Info,
    title = "Title",
    description = "Important information"
)
```

### Empty States
```kotlin
EmptyStateView(
    icon = Icons.Outlined.Folder,
    title = "No Items",
    description = "Add items to get started",
    action = { Button(...) }
)
```

### Section Headers
```kotlin
SectionHeaderCard(
    icon = Icons.Outlined.Category,
    title = "Section Title",
    description = "Section description"
)
```

## UI Patterns

### Screen Structure
1. **TopAppBar**: Screen title with navigation/actions
2. **Content**: Scrollable content with proper padding
3. **Bottom Bar/FAB**: Primary actions

### Form Inputs
- Use `OutlinedTextField` for most inputs
- Provide clear labels and placeholders
- Add supporting text for additional context
- Show validation errors inline

### Lists
- Use `LazyColumn` for scrollable lists
- Add proper spacing with `Arrangement.spacedBy()`
- Include empty states for no content
- Add swipe actions where appropriate

### Dialogs & Bottom Sheets
- Use dialogs for confirmations and simple inputs
- Use bottom sheets for complex selections or multiple options
- Always provide cancel/dismiss options

## Animation Guidelines
- Use `animateContentSize()` for size changes
- Apply spring animations for natural motion
- Keep animations subtle and functional
- Duration: 200-300ms for most transitions

## Icon Usage
- Use Material Icons Outlined variant
- Use AutoMirrored variants for directional icons
- Standard sizes: 20dp (inline), 24dp (standard), 72dp (empty states)

## Best Practices
1. **State Management**: Hoist state to ViewModels
2. **Recomposition**: Use `remember` and `derivedStateOf` appropriately
3. **Accessibility**: Add content descriptions to all icons
4. **Testing**: Test on both light and dark themes
5. **Performance**: Profile composables with Layout Inspector

## Component Checklist
- [ ] Uses Material 3 components
- [ ] Follows color system guidelines
- [ ] Has proper spacing and padding
- [ ] Includes accessibility features
- [ ] Handles empty/error states
- [ ] Animates state changes smoothly
- [ ] Works in both light/dark themes
- [ ] Handles different screen sizes