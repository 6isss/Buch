package app.areada.ui.applebooks

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.areada.R
import app.areada.data.BookNoteLink
import app.areada.data.BookStatus
import app.areada.data.library.LibraryBookEntry
import app.areada.data.library.LibraryFileFilter
import app.areada.data.library.LibraryFolderEntry
import app.areada.data.library.LibraryFolderPickerEntry
import app.areada.data.library.LibraryRoot
import app.areada.data.library.LibrarySearchResult
import app.areada.data.library.LibrarySearchResultType
import app.areada.data.library.LibrarySortMode
import app.areada.data.preview.BookPreviewText
import app.areada.data.reader.DocumentType
import app.areada.data.reader.ReaderPreferences
import app.areada.data.reader.ReadingBookmark
import app.areada.data.reader.ReadingProgress
import app.areada.data.reader.RecentDocument
import app.areada.ui.home.HomeScreen
import app.areada.ui.reader.LibraryScrollPosition
import app.areada.ui.reader.ReaderSettingsSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Apple Books shell: three tabs (Reading Now, Library, Search) over the existing
 * reader engine. The original folder browser is still reachable as "Collections"
 * so sorting, filters, pinning, notes and bookmarks keep working untouched.
 */
@Composable
internal fun AppleHome(
    roots: List<LibraryRoot>,
    folderPickerEntries: List<LibraryFolderPickerEntry>,
    selectedRootUriString: String?,
    currentRelativePath: String,
    folders: List<LibraryFolderEntry>,
    books: List<LibraryBookEntry>,
    searchQuery: String,
    searchResults: List<LibrarySearchResult>,
    isSearching: Boolean,
    recents: List<RecentDocument>,
    bookmarks: List<ReadingBookmark>,
    preferences: ReaderPreferences,
    sortMode: LibrarySortMode,
    fileFilter: LibraryFileFilter,
    selectedHomeTabName: String,
    folderDocumentTypesById: Map<String, Set<DocumentType>>,
    progressByUri: Map<String, ReadingProgress>,
    bookStatusByUri: Map<String, BookStatus>,
    bookNoteLinksByUri: Map<String, BookNoteLink>,
    pinnedLibraryItemIds: Set<String>,
    libraryScrollPositions: MutableMap<String, LibraryScrollPosition>,
    onChooseFolder: () -> Unit,
    onOpenFile: () -> Unit,
    onRefresh: () -> Unit,
    onSelectRoot: (LibraryRoot) -> Unit,
    onRemoveRoot: (LibraryRoot) -> Unit,
    onOpenPickerEntry: (LibraryFolderPickerEntry) -> Unit,
    onCreateTextNote: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenSearchResult: (LibrarySearchResult) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenBook: (LibraryBookEntry) -> Unit,
    onOpenRecent: (RecentDocument) -> Unit,
    onOpenBookmark: (ReadingBookmark) -> Unit,
    onRemoveBookmark: (ReadingBookmark) -> Unit,
    onRemoveRecent: (RecentDocument) -> Unit,
    onMoveBookmark: (ReadingBookmark, Int) -> Unit,
    onMoveRecent: (RecentDocument, Int) -> Unit,
    onRenameBookmark: (ReadingBookmark, String) -> Unit,
    onSortModeChange: (LibrarySortMode) -> Unit,
    onFileFilterChange: (LibraryFileFilter) -> Unit,
    onHomeTabChange: (String) -> Unit,
    onDeleteFolder: (LibraryFolderEntry) -> Unit,
    onDeleteBook: (LibraryBookEntry) -> Unit,
    onRenameFolder: (LibraryFolderEntry, String) -> Unit,
    onRenameBook: (LibraryBookEntry, String) -> Unit,
    onTogglePinFolder: (LibraryFolderEntry) -> Unit,
    onTogglePinBook: (LibraryBookEntry) -> Unit,
    onTogglePinDocument: (String) -> Unit,
    onUpdateBookStatus: (String, BookStatus) -> Unit,
    onPreferencesChange: (ReaderPreferences) -> Unit,
) {
    var showCollections by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(AppleTab.ReadingNow) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showCollections) {
        BackHandler { showCollections = false }

        HomeScreen(
            roots = roots,
            folderPickerEntries = folderPickerEntries,
            selectedRootUriString = selectedRootUriString,
            currentRelativePath = currentRelativePath,
            folders = folders,
            books = books,
            searchQuery = searchQuery,
            searchResults = searchResults,
            isSearching = isSearching,
            recents = recents,
            bookmarks = bookmarks,
            preferences = preferences,
            sortMode = sortMode,
            fileFilter = fileFilter,
            selectedHomeTabName = selectedHomeTabName,
            folderDocumentTypesById = folderDocumentTypesById,
            progressByUri = progressByUri,
            bookStatusByUri = bookStatusByUri,
            bookNoteLinksByUri = bookNoteLinksByUri,
            pinnedLibraryItemIds = pinnedLibraryItemIds,
            libraryScrollPositions = libraryScrollPositions,
            onChooseFolder = onChooseFolder,
            onOpenFile = onOpenFile,
            onRefresh = onRefresh,
            onSelectRoot = onSelectRoot,
            onRemoveRoot = onRemoveRoot,
            onOpenPickerEntry = onOpenPickerEntry,
            onCreateTextNote = onCreateTextNote,
            onSearchQueryChange = onSearchQueryChange,
            onOpenSearchResult = onOpenSearchResult,
            onOpenFolder = onOpenFolder,
            onOpenBook = onOpenBook,
            onOpenRecent = onOpenRecent,
            onOpenBookmark = onOpenBookmark,
            onRemoveBookmark = onRemoveBookmark,
            onRemoveRecent = onRemoveRecent,
            onMoveBookmark = onMoveBookmark,
            onMoveRecent = onMoveRecent,
            onRenameBookmark = onRenameBookmark,
            onSortModeChange = onSortModeChange,
            onFileFilterChange = onFileFilterChange,
            onHomeTabChange = onHomeTabChange,
            onDeleteFolder = onDeleteFolder,
            onDeleteBook = onDeleteBook,
            onRenameFolder = onRenameFolder,
            onRenameBook = onRenameBook,
            onTogglePinFolder = onTogglePinFolder,
            onTogglePinBook = onTogglePinBook,
            onTogglePinDocument = onTogglePinDocument,
            onUpdateBookStatus = onUpdateBookStatus,
            onPreferencesChange = onPreferencesChange,
        )
        return
    }

    val avatarContext = LocalContext.current
    val avatarState = remember { AvatarState(AvatarStore.getUriString(avatarContext)) }
    LaunchedEffect(avatarState.uriString) {
        val value = avatarState.uriString
        avatarState.bitmap = if (value == null) {
            null
        } else {
            AvatarStore.cached(value)?.asImageBitmap()
                ?: withContext(Dispatchers.IO) { AvatarStore.load(avatarContext, value)?.asImageBitmap() }
        }
    }

    AppleBooksTheme {
        CompositionLocalProvider(LocalAvatarState provides avatarState) {
        if (showSettings) {
            ReaderSettingsSheet(
                preferences = preferences,
                showReadingControls = false,
                showLanguageSelector = true,
                showGuideIconToggle = true,
                onDismiss = { showSettings = false },
                onPreferencesChange = onPreferencesChange,
            )
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                AppleBottomBar(
                    selected = tab,
                    labels = { entry -> tabLabel(entry) },
                    onSelect = { selected -> tab = selected },
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding()),
            ) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200))
                            .togetherWith(fadeOut(animationSpec = tween(200)))
                    },
                    label = "appleTab",
                ) { current ->
                val tabBlur by transition.animateDp(
                    transitionSpec = { tween(200) },
                    label = "appleTabBlur",
                ) { state -> if (state == EnterExitState.Visible) 0.dp else 9.dp }
                Box(modifier = Modifier.blur(tabBlur)) {
                when (current) {
                    AppleTab.ReadingNow -> ReadingNowTab(
                        recents = recents,
                        progressByUri = progressByUri,
                        onOpenRecent = onOpenRecent,
                        onRemoveRecent = onRemoveRecent,
                        onOpenSettings = { showSettings = true },
                        onBrowse = { showCollections = true },
                        onOpenFile = onOpenFile,
                    )

                    AppleTab.Library -> LibraryTab(
                        books = books,
                        progressByUri = progressByUri,
                        hasRoots = roots.isNotEmpty(),
                        onOpenBook = onOpenBook,
                        onOpenCollections = { showCollections = true },
                        onOpenSettings = { showSettings = true },
                        onChooseFolder = onChooseFolder,
                        onOpenFile = onOpenFile,
                    )

                    AppleTab.Search -> SearchTab(
                        query = searchQuery,
                        results = searchResults,
                        onQueryChange = onSearchQueryChange,
                        onOpenResult = onOpenSearchResult,
                    )
                }
                }
                }
            }
        }
        }
    }
}

/** Avatar image kept above tab switching so it never re-decodes or flashes. */
internal class AvatarState(initial: String?) {
    var uriString by mutableStateOf(initial)
    var bitmap by mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(
        AvatarStore.cached(initial)?.asImageBitmap(),
    )
}

internal val LocalAvatarState = staticCompositionLocalOf<AvatarState?> { null }

@Composable
private fun tabLabel(tab: AppleTab): String = when (tab) {
    AppleTab.ReadingNow -> stringResource(R.string.apple_reading_now)
    AppleTab.Library -> stringResource(R.string.apple_library)
    AppleTab.Search -> stringResource(R.string.apple_search)
}

// region Reading Now

@Composable
private fun ReadingNowTab(
    recents: List<RecentDocument>,
    progressByUri: Map<String, ReadingProgress>,
    onOpenRecent: (RecentDocument) -> Unit,
    onRemoveRecent: (RecentDocument) -> Unit,
    onOpenSettings: () -> Unit,
    onBrowse: () -> Unit,
    onOpenFile: () -> Unit,
) {
    val context = LocalContext.current
    val current = recents.firstOrNull()
    var previewText by remember(current?.uriString) { mutableStateOf<String?>(null) }
    var loadingPreview by remember(current?.uriString) { mutableStateOf(current != null) }

    LaunchedEffect(current?.uriString) {
        val document = current
        if (document == null) {
            loadingPreview = false
            return@LaunchedEffect
        }
        loadingPreview = true
        previewText = BookPreviewText.load(
            context = context,
            uriString = document.uriString,
            type = document.type,
            progress = progressByUri[document.uriString],
        )
        loadingPreview = false
    }

    val gutter = Modifier.padding(horizontal = 20.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(modifier = gutter) {
            AppleHeader(
                title = stringResource(R.string.apple_reading_now),
                onProfileClick = onOpenSettings,
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        if (current == null) {
            Box(modifier = gutter) {
                EmptyState(
                    message = stringResource(R.string.apple_no_reading),
                    actionLabel = stringResource(R.string.apple_open_file),
                    onAction = onOpenFile,
                    secondaryLabel = stringResource(R.string.apple_collections),
                    onSecondary = onBrowse,
                )
            }
        } else {
            OpenBookCard(
                previewText = previewText,
                loading = loadingPreview,
                modifier = gutter,
                onClick = { onOpenRecent(current) },
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = gutter,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = current.title.substringBeforeLast('.'),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = progressLabel(progressByUri[current.uriString]),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BookOverflowMenu(
                    onOpen = { onOpenRecent(current) },
                    onRemove = { onRemoveRecent(current) },
                )
            }

            Spacer(modifier = Modifier.height(26.dp))
            HorizontalDivider(
                modifier = gutter,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.apple_recently_opened),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.2.em,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = gutter,
            )

            Spacer(modifier = Modifier.height(14.dp))

            val rest = recents.drop(1)
            if (rest.isEmpty()) {
                Text(
                    text = stringResource(R.string.apple_recently_opened_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = gutter,
                )
            } else {
                // full-bleed carousel: covers run off both screen edges
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                ) {
                    items(rest, key = { recent -> recent.uriString }) { recent ->
                        Column(modifier = Modifier.width(104.dp)) {
                            PhysicalBookCover(
                                uriString = recent.uriString,
                                title = recent.title,
                                type = recent.type,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenRecent(recent) },
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ProgressLine(progress = progressByUri[recent.uriString])
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// endregion

// region Library

@Composable
private fun LibraryTab(
    books: List<LibraryBookEntry>,
    progressByUri: Map<String, ReadingProgress>,
    hasRoots: Boolean,
    onOpenBook: (LibraryBookEntry) -> Unit,
    onOpenCollections: () -> Unit,
    onOpenSettings: () -> Unit,
    onChooseFolder: () -> Unit,
    onOpenFile: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        AppleHeader(
            title = stringResource(R.string.apple_library),
            onProfileClick = onOpenSettings,
            trailing = {
                TextButton(onClick = onOpenCollections) {
                    Text(
                        text = stringResource(R.string.apple_edit),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenCollections)
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.apple_collections),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        if (books.isEmpty()) {
            EmptyState(
                message = if (hasRoots) {
                    stringResource(R.string.apple_empty_folder)
                } else {
                    stringResource(R.string.apple_empty_library)
                },
                actionLabel = stringResource(R.string.apple_add_folder),
                onAction = onChooseFolder,
                secondaryLabel = stringResource(R.string.apple_open_file),
                onSecondary = onOpenFile,
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.widthIn(max = 520.dp),
                horizontalArrangement = Arrangement.spacedBy(26.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(26.dp),
                contentPadding = PaddingValues(top = 22.dp, bottom = 40.dp),
            ) {
                items(books, key = { book -> book.id }) { book ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PhysicalBookCover(
                            uriString = book.uriString,
                            title = book.title,
                            type = book.type,
                            elevation = 25.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenBook(book) },
                        )
                        Spacer(modifier = Modifier.height(9.dp))
                        Text(
                            text = book.title.substringBeforeLast('.'),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ProgressLine(progress = progressByUri[book.uriString])
                    }
                }
            }
            }
        }
    }
}

// endregion

// region Search

@Composable
private fun SearchTab(
    query: String,
    results: List<LibrarySearchResult>,
    onQueryChange: (String) -> Unit,
    onOpenResult: (LibrarySearchResult) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val dismissInteraction = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = dismissInteraction,
                indication = null,
            ) {
                focusManager.clearFocus()
                keyboard?.hide()
            }
            .padding(horizontal = 20.dp),
    ) {
        AppleHeader(title = stringResource(R.string.apple_search))

        Spacer(modifier = Modifier.height(22.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
            },
            placeholder = { Text(text = stringResource(R.string.apple_search_placeholder)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black.copy(alpha = 0.30f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.onBackground,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(18.dp))

        val books = results.filter { result -> result.type == LibrarySearchResultType.BOOK }

        if (query.isBlank()) {
            Unit
        } else if (books.isEmpty()) {
            Text(
                text = stringResource(R.string.apple_no_results),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            items(books, key = { result -> result.id }) { result ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    PhysicalBookCover(
                        uriString = result.uriString.orEmpty(),
                        title = result.title,
                        type = result.documentType ?: DocumentType.TXT,
                        elevation = 25.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenResult(result) },
                    )
                    Spacer(modifier = Modifier.height(9.dp))
                    Text(
                        text = result.title.substringBeforeLast('.'),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = result.rootName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    }
                }
            }
        }
    }
}

// endregion

// region Shared pieces

@Composable
private fun AppleHeader(
    title: String,
    onProfileClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Column(modifier = Modifier.statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 30.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.weight(1f),
            )
            trailing?.invoke()
            if (onProfileClick != null) {
                Spacer(modifier = Modifier.width(6.dp))
                ProfileAvatar(onClick = onProfileClick)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

/** Circular profile button: tap opens settings, long press picks a photo. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileAvatar(onClick: () -> Unit) {
    val context = LocalContext.current
    val shared = LocalAvatarState.current
    val fallback = remember { AvatarState(AvatarStore.getUriString(context)) }
    val state = shared ?: fallback

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { picked ->
        if (picked != null) {
            AvatarStore.set(context, picked)
            state.uriString = picked.toString()
        }
    }

    LaunchedEffect(state.uriString) {
        if (state.bitmap == null) {
            val value = state.uriString
            state.bitmap = if (value == null) {
                null
            } else {
                withContext(Dispatchers.IO) {
                    AvatarStore.load(context, value)?.asImageBitmap()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val image = state.bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = stringResource(R.string.apple_settings),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = stringResource(R.string.apple_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

/** Percent read, or a blue "NEW" pill for books that were never opened. */
@Composable
private fun ProgressLine(progress: ReadingProgress?) {
    val percent = progressPercent(progress)
    if (percent == null) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFF0040A8))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = stringResource(R.string.apple_new_badge),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.08.em,
            )
        }
    } else {
        Text(
            text = stringResource(R.string.apple_percent_read, percent),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun BookOverflowMenu(
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.apple_continue_reading)) },
                onClick = {
                    expanded = false
                    onOpen()
                },
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.apple_remove_from_recent)) },
                onClick = {
                    expanded = false
                    onRemove()
                },
            )
        }
    }
}

@Composable
private fun EmptyState(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onAction) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = actionLabel, color = MaterialTheme.colorScheme.secondary)
        }
        if (secondaryLabel != null && onSecondary != null) {
            TextButton(onClick = onSecondary) {
                Text(text = secondaryLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun progressLabel(progress: ReadingProgress?): String {
    val percent = progressPercent(progress)
    return if (percent == null) {
        stringResource(R.string.apple_not_started)
    } else {
        stringResource(R.string.apple_percent_read, percent)
    }
}

private fun progressPercent(progress: ReadingProgress?): Int? {
    if (progress == null) return null

    val fraction = when (progress.type) {
        DocumentType.EPUB, DocumentType.FB2, DocumentType.MARKDOWN -> {
            if (progress.epubChapterCount <= 0) {
                null
            } else {
                (progress.epubChapterIndex + progress.epubScrollFraction) / progress.epubChapterCount
            }
        }

        DocumentType.PDF -> {
            if (progress.pdfPageCount <= 0) {
                null
            } else {
                (progress.pdfPageIndex + 1f) / progress.pdfPageCount
            }
        }

        else -> progress.txtScrollFraction
    } ?: return null

    val percent = (fraction * 100f).toInt().coerceIn(0, 100)
    return if (percent <= 0) null else percent
}

// endregion
