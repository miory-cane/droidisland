# Google Play Store Release & Submission Kit

Комплект метаданных, юридических обоснований и формуляров для публикации **DroidIsland** в **Google Play Console** на основе актуальной кодовой базы.

---

## 1. Основные метаданные (Store Listing Metadata)

### Язык: Русский (ru-RU)
- **Название приложения (App title, до 30 символов):**
  `DroidIsland: Dynamic Island`
- **Краткое описание (Short description, до 80 символов):**
  `Эффект расширения камеры, динамический остров и виджеты для вашего Android.`
- **Полное описание (Full description, до 4000 символов):**
```text
DroidIsland превращает вырез фронтальной камеры вашего Android-смартфона в стильный, плавный и многофункциональный интерактивный элемент с выразительным темным дизайном Material You / Google Pixel.

🖤 ЭФФЕКТ РАСШИРЕНИЯ И ПОГЛОЩЕНИЯ КАМЕРЫ (NO DOUBLE CUTOUT)
• В состоянии покоя остров полностью скрыт внутри объектива фронтальной камеры и не занимает полезного места на экране.
• 100% OLED Black маска в верхней части островка обеспечивает бесшовное поглощение объектива без искусственных рамок и раздвоений.
• При событиях или касании остров плавно и органично «вырастает» из камеры благодаря физическим пружинным анимациям.

🎵 ИНТЕРАКТИВНЫЙ МЕДИАПЛЕЕР
• Отображение активного трека из Spotify, YouTube Music, Apple Music и других плееров.
• Живая динамическая палитра: фоновые акценты извлекаются прямо из обложки текущего альбома.
• Анимированный бас-визуализатор (Bouncing Bass) и экспрессивный волнистый ползунок трека (Squiggly Slider).
• Быстрое управление: пауза/воспроизведение, переключение треков и запуск музыкального приложения в одно касание.

🎧 BLUETOOTH И TWS НАУШНИКИ (FAST PAIR СТИЛЬ)
• Красивые всплывающие карточки при подключении TWS-наушников (Pixel Buds, AirPods, Galaxy Buds).
• Раздельная индикация уровня заряда: Левый наушник, Правый наушник и Зарядный кейс.
• Поддержка полноразмерных наушников (Over-Ear) с переключением режимов шумоподавления (ANC / Прозрачность).

🔋 ИНДИКАТОР ЗАРЯДКИ И БАТАРЕИ
• Мгновенный импульс и статусная пилюля при подключении смартфона к зарядному устройству.
• Четкая индикация текущего процента заряда аккумулятора.

🔕 СИСТЕМНЫЙ СТАТУС-HUD
• Быстрая анимация при переключении звуковых профилей («Без звука / Не беспокоить», «Вибрация», «Обычный звонок»).

🎯 ТОЧНАЯ КАЛИБРОВКА ПОД ЛЮБОЙ СМАРТФОН
• Готовые пресеты: камера по центру, камера слева, двойная пилюля (dual pill) или камера справа.
• Точная настройка сдвига по осям X и Y, диаметра глазка камеры, ширины и высоты шторки.
• Визуальный прицел для выравнивания островка с точностью до миллиметра.
• Интерактивная песочница «Тесты» для мгновенной проверки всех видов анимаций.

🔒 100% ПРИВАТНОСТЬ И БЕЗОПАСНОСТЬ
• Приложение работает полностью локально на вашем смартфоне.
• Никаких трекеров, аналитики, рекламы и отправки данных в интернет.
• Все настройки сохраняются только на вашем устройстве в Jetpack DataStore.

💖 Поддержать автора и разработку: https://boosty.to/droidisland_miory
```

---

### Язык: Английский (en-US)
- **App title (up to 30 characters):**
  `DroidIsland: Dynamic Island`
- **Short description (up to 80 characters):**
  `Dynamic camera cutout expansion, music island, and status HUD for Android.`
- **Full description (up to 4000 characters):**
```text
DroidIsland transforms your Android smartphone's camera punch-hole cutout into a fluid, responsive, and gorgeous interactive dynamic island crafted with a dark Material You / Google Pixel aesthetic.

🖤 SEAMLESS CAMERA CUTOUT ABSORPTION
• In idle state, the island is completely concealed inside the physical camera punch-hole, taking zero extra screen real estate.
• 100% OLED black top mask ensures pure seamless absorption of your physical camera lens with zero artificial halos or double-cutout glitches.
• Organically expands outward from the camera lens when events occur using high-fidelity spring physics.

🎵 DYNAMIC MEDIA PLAYER
• Live playback status from Spotify, YouTube Music, Apple Music, and other media players.
• Dynamic color palette extracted in real-time from active album artwork.
• Animated Bouncing Bass visualizer and expressive Squiggly Wave progress bar.
• Interactive playback controls: Play/Pause, Next/Previous, and tap to open music player.

🎧 BLUETOOTH & TWS POPUPS (FAST PAIR STYLE)
• Stylish popup cards when connecting Bluetooth audio accessories (AirPods, Pixel Buds, Galaxy Buds).
• Independent battery readouts for Left earbud, Right earbud, and Charging Case.
• Over-Ear headphone support with interactive Active Noise Cancellation (ANC) mode toggling.

🔋 BATTERY CHARGING HUD
• Instant dynamic status capsule triggered upon plugging in charger.
• Real-time battery percentage indicator with green charging bolt.

🔕 SOUND PROFILE HUD
• Instant visual feedback when toggling between Silent (DND), Vibrate, and Normal ringtone modes.

🎯 PRECISION CALIBRATION FOR ANY PHONE
• Instant presets for Center Punch-Hole, Left Punch-Hole, Dual Pill, and Right Punch-Hole cutouts.
• Fine-tuning sliders for X/Y position offsets, camera diameter, expanded width (up to 600 dp), and height.
• Live calibration crosshair to align the island with sub-millimeter precision.
• Built-in "Tests" sandbox to preview all animations and states instantly.

🔒 100% PRIVATE & ON-DEVICE
• Operates completely offline on your device.
• Zero data collection, no telemetry, no tracking IDs, and no third-party ads.
• Configuration stored strictly in local Jetpack DataStore.

💖 Support project development: https://boosty.to/droidisland_miory
```

---

## 2. Формуляры для Google Play App Review (Декларации разрешений)

### А. Декларация Accessibility Service (`BIND_ACCESSIBILITY_SERVICE`)
- **What core user-facing functionality requires the AccessibilityService?**
  ```text
  DroidIsland uses the AccessibilityService API strictly to detect system window state changes (such as full-screen video playback, gaming, and lockscreen transitions) in order to align the dynamic island overlay with the hardware camera cutout and temporarily hide the overlay when interactive apps request exclusive fullscreen focus.
  ```

- **Does your app collect or share any personal or sensitive user data using AccessibilityService?**
  ```text
  No. DroidIsland does not collect, log, store, or transmit any user keystrokes, screen text, input events, or personal information. The accessibility events are processed entirely in memory on the device to adjust overlay positioning and dismiss the capsule when interactive full-screen applications request exclusive focus.
  ```

- **Video demonstration URL for reviewers:**
  *(Приложите ссылку на YouTube видео (Unlisted), показывающее работу островка и экран настроек, где четко виден диалог согласия пользователя).*

---

### Б. Декларация Foreground Service Special Use (`FOREGROUND_SERVICE_SPECIAL_USE`)
- **Which user-facing feature requires a foreground service?**
  ```text
  Floating camera cutout status overlay (Dynamic Island). The foreground service maintains the real-time floating overlay over the camera punch-hole, displaying active media playback controls, charging animation status, and connected Bluetooth accessories.
  ```

- **Why can't this feature be completed with WorkManager, JobScheduler, or when the app is in the foreground?**
  ```text
  The dynamic island must remain persistently visible across third-party applications while the user interacts with their device. Without a foreground service, the Android system would immediately kill the overlay window when the main settings activity is minimized.
  ```

---

### В. Декларация Bluetooth Connect (`BLUETOOTH_CONNECT`)
- **Usage justification:**
  ```text
  DroidIsland uses BLUETOOTH_CONNECT strictly with the 'neverForLocation' attribute flag to display the connection status, accessory name, and battery level of connected audio peripherals (headphones and earbuds) directly inside the dynamic island capsule. The app does not derive or access user location.
  ```

---

### Г. Декларация Notification Listener (`BIND_NOTIFICATION_LISTENER_SERVICE`)
- **Usage justification:**
  ```text
  DroidIsland uses the NotificationListenerService to read playback metadata (song title, artist, playback state, and album artwork) from active media sessions in order to render playback controls and animated visualizers in the expanded island capsule. No notification contents outside media sessions are read, collected, or stored.
  ```

---

## 3. Анкета безопасности данных (Data Safety Form)

При заполнении анкеты в Google Play Console выберите следующие пункты:

1. **Сбор и передача данных (Data Collection & Sharing):**
   - Собирает ли приложение какие-либо типы данных? -> **Нет (No)**.
   - Передает ли приложение данные третьим лицам? -> **Нет (No)**.
2. **Безопасность (Security Practices):**
   - Передаются ли данные по зашифрованному соединению? -> **Данные не передаются (N/A)**.
   - Возможность удаления данных: **Да (Все данные хранятся локально и удаляются при очистке данных приложения или его деинсталляции)**.
3. **Соответствие требованиям семейной политики (Target Age & Families):**
   - Целевая аудитория: **13 лет и старше**.

---

## 4. Графические материалы (Graphic Assets Specifications)

| Материал | Разрешение | Формат | Требования |
|---|---|---|---|
| **Иконка приложения** | 512 × 512 px | PNG 32-bit | Непрозрачный фон, до 1024 КБ (готово в `store_assets/icon_playstore_512.png`) |
| **Feature Graphic** | 1024 × 500 px | JPG или PNG | Горизонтальный промо-баннер без альфа-канала с демонстрацией островка над камерой |
| **Скриншоты для телефонов** | Мин. 2 шт. (1080 × 2400 px) | PNG / JPG | 1) Главный экран Островка с превью и статусом службы<br>2) Экран Триггеров и экран Калибровки с прицелом |
