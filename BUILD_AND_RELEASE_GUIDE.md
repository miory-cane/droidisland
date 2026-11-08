# Полное руководство по релизу DroidIsland (GitHub & Google Play Console)

Это исчерпывающее руководство содержит пошаговый план вывода проекта **DroidIsland** в публичный доступ на **GitHub** и публикации в **Google Play Store**.

---

## ЧАСТЬ 1. ПУБЛИКАЦИЯ РЕПОЗИТОРИЯ НА GITHUB

### 1.1. Подготовка локального Git-репозитория
Проект уже содержит корректный `.gitignore`, исключающий скомпилированные артефакты, локальные ключи и кэши (`local.properties`, `*.apk`, `*.aab`, `*.jks`, `.gradle`, `.idea`).

Выполните следующие команды в терминале:
```powershell
# Переход в корневую папку проекта
cd "C:\Users\Miory Cane\.gemini\antigravity\scratch\DroidIsland"

# Проверка статуса файлов
git status

# Добавление всех файлов в индекс
git add .

# Создание коммита релиза
git commit -m "feat(release): prepare DroidIsland for GitHub and Google Play production launch"

# Переименование главной ветки в main (стандарт GitHub)
git branch -M main
```

### 1.2. Создание репозитория на GitHub
1. Перейдите на [github.com/new](https://github.com/new).
2. Задайте имя репозитория: `DroidIsland`.
3. Описание: `Organic True Liquid Glass dynamic island for Android with FlclashX aesthetics`.
4. Выберите тип: **Public** (Публичный).
5. **Не** инициализируйте репозиторий с README или .gitignore (они уже созданы локально!).
6. Нажмите **Create repository**.

### 1.3. Отправка кода (Push)
Скопируйте URL созданного репозитория и выполните:
```powershell
# Привязка удаленного репозитория (замените YOUR_USERNAME на ваш GitHub логин)
git remote add origin https://github.com/miory-cane/DroidIsland.git

# Отправка ветки main
git push -u origin main
```

### 1.4. Автоматическая сборка в GitHub Actions
В папку `.github/workflows/build.yml` уже добавлен сценарий CI/CD:
- При каждом `push` в ветку `main` GitHub Actions автоматически разворачивает окружение с JDK 17, запускает `./gradlew check`, собирает отладочный и релизный APK, а также готовый бандл `app-release.aab`.
- Готовые файлы доступны для скачивания во вкладке **Actions** -> **Artifacts** каждого запуска.

### 1.5. Публикация первого GitHub Release
1. В вашем репозитории на GitHub перейдите в раздел **Releases** (справа) -> **Draft a new release**.
2. Укажите тэг: `v1.0.0`.
3. Заголовок: `DroidIsland v1.0.0 — True Liquid Glass & FlclashX UI`.
4. В поле описания вставьте список изменений:
   ```markdown
   ### What's New:
   - 💎 True Liquid Glass optics powered by AGSL RuntimeShader (refraction, chromatic aberration, rim light).
   - 🖤 Monolithic camera cutout absorption: eliminates double-camera ring glitch.
   - 🌊 Organic spring physics (squish & stretch elastic motion).
   - 🎛️ FlclashX / Cupertino desktop-grade control center with glass accordions.
   - 🎵 Dynamic media player, charging animations, and Fast Pair Bluetooth badges.
   - 🔒 100% On-device privacy with zero telemetry.
   ```
5. Прикрепите собранные файлы:
   - `app-release.apk`
   - `app-release.aab`
6. Нажмите **Publish release**.

---

## ЧАСТЬ 2. ГЕНЕРАЦИЯ ЦИФРОВОГО КЛЮЧА ПОДПИСИ (KEYSTORE)

Google Play требует обязательную криптографическую подпись каждого `.aab` пакета.

### Вариант А: Генерация ключа через командную строку (Keytool)
Запустите PowerShell и выполните команду (используя утилиту `keytool` из Android Studio JBR):
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
& "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -v `
  -keystore "C:\Users\Miory Cane\droidisland-release-key.jks" `
  -alias "droidisland" `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000 `
  -storepass "YourStrongPassword123" `
  -keypass "YourStrongPassword123" `
  -dname "CN=DroidIsland, OU=Mobile, O=DroidIsland, L=City, ST=State, C=US"
```
> [!IMPORTANT]
> Сохраните файл `droidisland-release-key.jks` и пароли в надёжное безопасное место (например, в менеджер паролей). Если ключ будет утерян, вы не сможете обновлять приложение в Google Play.

### Вариант Б: Генерация ключа в Android Studio UI
1. Меню **Build** -> **Generate Signed Bundle / APK...**
2. Выберите **Android App Bundle** -> **Next**.
3. Нажмите **Create new...** под полем Key store path.
4. Укажите путь сохранения, пароли и имя алиаса (`droidisland`).
5. Нажмите **OK** -> **Next** -> выберите вариант **release** -> **Finish**.

---

## ЧАСТЬ 3. СБОРКА РЕЛИЗНОГО ПАКЕТА (AAB)

Собрать готовый релизный App Bundle с оптимизацией R8 / ProGuard:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew bundleRelease assembleRelease
```
Результаты сборки:
- **App Bundle для Google Play:** `app/build/outputs/bundle/release/app-release.aab`
- **Универсальный APK для тестов и GitHub:** `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## ЧАСТЬ 4. ПОШАГОВАЯ ПУБЛИКАЦИЯ В GOOGLE PLAY CONSOLE

### Шаг 1. Создание приложения
1. Войдите в [Google Play Console](https://play.google.com/console).
2. Нажмите синюю кнопку **Создать приложение** (Create app).
3. Заполните поля:
   - **Название:** `DroidIsland: Dynamic Island`
   - **Язык по умолчанию:** Русский (ru-RU) или Английский (en-US)
   - **Тип:** Приложение (App)
   - **Бесплатное или платное:** Бесплатное (Free)
4. Примите условия и нажмите **Создать приложение**.

---

### Шаг 2. Заполнение обязательных анкет (App Content)
В левом боковом меню перейдите в **Политика и программы** (Policy and programs) -> **Содержимое приложения** (App content):

1. **Политика конфиденциальности (Privacy policy):**
   - Укажите прямую ссылку на размещенный файл `PRIVACY_POLICY.md` (например, сырая ссылка на GitHub `https://raw.githubusercontent.com/miory-cane/DroidIsland/main/PRIVACY_POLICY.md` или страница GitHub Pages).
2. **Доступ к приложениям (App access):**
   - Выберите: *Все функции доступны без ограничений* (All functionality is available without special access).
3. **Реклама (Ads):**
   - Выберите: *Нет, в моем приложении нет рекламы* (No, my app does not contain ads).
4. **Возрастной рейтинг (Content ratings):**
   - Заполните анкету IARC: тип «Утилиты / Прочее» -> на все вопросы о насилии, нецензурной лексике и сборе данных ответьте «Нет». Итоговый рейтинг: **3+ / PEGI 3 / Everyone**.
5. **Целевая аудитория (Target audience):**
   - Выберите: **13 лет и старше**.
6. **Безопасность данных (Data Safety):**
   - На вопрос *«Собирает или передает ли приложение данные пользователей?»* выберите **НЕТ**.
   - (Подробные формуляры приведены в файле `PLAY_STORE_METADATA.md`).

---

### Шаг 3. Декларации спецразрешений (Критически важный шаг!)

В разделе **App content** заполните декларации для:

#### 1. Декларация службы специальных возможностей (Accessibility Service):
- Google запросит видеодемонстрацию и текст обоснования.
- Скопируйте готовый текст из раздела 2.А файла `PLAY_STORE_METADATA.md`.
- Запишите короткое видео (1-2 минуты, снимите экран смартфона), где видно:
  1) Экран приветствия с запросом разрешения.
  2) Как пользователь активирует переключатель в настройках Android.
  3) Как островок плавно реагирует на события.
  Загрузите видео на YouTube как **Доступ по ссылке (Unlisted)** и вставьте URL в форму.

#### 2. Декларация Foreground Service Special Use:
- Скопируйте готовый текст из раздела 2.Б файла `PLAY_STORE_METADATA.md`.

---

### Шаг 4. Оформление страницы приложения в магазине (Store Listing)
Перейдите в **Рост** -> **Присутствие в Google Play** -> **Основная страница приложения**:
1. Вставьте тексты заголовка, краткого и полного описания из файла `PLAY_STORE_METADATA.md`.
2. Загрузите графику:
   - **Иконка (512x512):** возьмите готовый файл `store_assets/icon_playstore_512.png`.
   - **Картинка для описания (Feature Graphic 1024x500):** баннер приложения.
   - **Скриншоты для телефонов (минимум 2):** скриншот экрана настроек FlclashX и скриншот калибровки/работы островка.

---

### Шаг 5. Загрузка выпуска и отправка на модерацию
1. В левом меню выберите **Выпуск** (Release) -> **Рабочий выпуск** (Production) (или **Закрытое тестирование** (Closed testing), если у вас новый личный аккаунт разработчика).
2. Нажмите **Создать новый выпуск** (Create new release).
3. Перетащите файл `app-release.aab`.
4. В поле **Примечания к выпуску (Release notes)** укажите:
   ```text
   Первый официальный релиз DroidIsland:
   - Поддержка True Liquid Glass оптики (AGSL шейдеры).
   - Бесшовное поглощение выреза камеры без раздвоения.
   - Пружинные анимации и центр управления FlclashX.
   ```
5. Нажмите **Сохранить** -> **Проверить выпуск** -> **Начать развертывание**!

Модерация Google Play для первого релиза обычно занимает от 1 до 5 рабочих дней.
