import axios from "axios";

const channelElement = document.getElementById("channelId") as HTMLInputElement;
const channelId: string = channelElement.value;

let ele = document.querySelector("meta[name='_csrf']") as HTMLElement;
const csrfToken = ele.getAttribute("content") as string;
ele = document.querySelector("meta[name='_csrf_header']") as HTMLElement;
const csrfHeader = ele.getAttribute("content") as string;
if (csrfToken && csrfHeader) {
	axios.defaults.headers.common[csrfHeader] = csrfToken;
} else {
	console.warn("CSRFトークンまたはヘッダー名が見つかりませんでした。CSRF保護が正しく機能しない可能性があります。");
}

const chatMessageContainer = document.getElementById(
	"chatMessageContainer"
) as HTMLDivElement;
const loadingMoreIndicator = document.getElementById(
	"loading-indicator"
) as HTMLDivElement; // 「さらに読み込み中...」表示用
const popupbox = document.getElementById("popup") as HTMLDivElement; // 「さらに読み込み中...」表示用

let isLoading = false; // 現在データ読み込み中かどうか
let allDataLoaded = false; // 全てのデータが読み込まれたかどうか
let page = 0; // 読み込むページのオフセット、または開始位置（サーバーサイドのAPI設計による）
const size = 100; // 一度に取得するアイテム数（サーバーサイドのAPIと合わせる）
let maxSize = 0;

window.addEventListener("DOMContentLoaded", (event: Event) => {
	const path: string = window.location.pathname;

	if (path == "/") {
		initIndex();
	} else if (path.includes("chatHtml")) {
		initChat();
	} else if (path.includes("memberHtml")) {
		initMember();
	} else if (path.includes("fightingStrengthHtml")) {
		initFightingStrength();
	} else {
		// その他のページ用の共通処理
	}
});

function initIndex() {
	existNewMessage();
}

function initChat() {
	// アプリケーション起動時にデータを取得して表示する関数を呼び出します
	fetchAndDisplayChatMessage(false);
	const button = document.getElementById("button") as HTMLInputElement;
	button.onclick = onPostMessage;
	//	setInterval(resetAndDisplayChatMessage, 5000);
}

function initMember() {
	fetchAndDisplayMember();
	const button = document.getElementById("deleteButton") as HTMLInputElement;
	button.onclick = onDeleteMember;
	const addButton = document.getElementById("addButton") as HTMLInputElement;
	addButton.onclick = onAddMember;
	const saveButton = document.getElementById("saveButton") as HTMLInputElement;
	saveButton.onclick = onSaveMember;
	const fightingStrengthDownloadButton = document.getElementById("fightingStrengthDownloadButton") as HTMLInputElement;
	fightingStrengthDownloadButton.onclick = onFightingStrengthDownload;
	const fightingStrengthUploadButton = document.getElementById("fightingStrengthUploadButton") as HTMLInputElement;
	fightingStrengthUploadButton.onclick = onFightingStrengthUpload;
	const fightingStrengthInitButton = document.getElementById("fightingStrengthInitButton") as HTMLInputElement;
	fightingStrengthInitButton.onclick = onFightingStrengthInit;
	
	
}
function initFightingStrength() {
	const fightingStrengthButton = document.getElementById("fightingStrengthButton") as HTMLInputElement;
	fightingStrengthButton.onclick = onSaveFightingStrength;
}

async function onFightingStrengthInit(): Promise<void> {
	await axios.get("/fightingStrength/init");
}
async function existNewMessage(): Promise<void> {
	
	const chatClasseElements = document.querySelectorAll<HTMLElement>('.chatClass');
	const elementsArray = Array.from(chatClasseElements);
	for (const htmlElement of elementsArray) {
		const channelId : string = htmlElement.dataset.channelId as string;
		const url = "/chat/existNewMessage?channelId="+channelId;
		const response = await axios.get(url, {
		});
		const isNew : Boolean = response.data;
		if (isNew) {
			htmlElement.style.color = 'red';
			const newEle = document.createElement("img") as HTMLImageElement;
			newEle.src = "/img/new.png";
			newEle.width = 30;
			htmlElement.appendChild(newEle);
		}
	}
}

async function onFightingStrengthDownload(): Promise<void> {
	const url = "/fightingStrength/excel";

	try {
		const response = await axios.get(url, {
			responseType: 'blob'
		});

		
		if (response.data instanceof Blob) {
			const blob: Blob = response.data;

			
			let filename: string = "fightingStrength.xslx";
			const contentDisposition: string = response.headers['content-disposition'];
			if (contentDisposition) {
				const filenameMatch = contentDisposition.match(/filename\*?=["']?([^"';]+)["']?/i);
				if (filenameMatch && filenameMatch.length > 1) {
					filename = decodeURIComponent(filenameMatch[1]);
				}
			}

			const downloadUrl = window.URL.createObjectURL(blob);

			const a = document.createElement('a') as HTMLAnchorElement;
			a.href = downloadUrl;
			a.download = filename;
			document.body.appendChild(a);
			a.click();
			document.body.removeChild(a);

			window.URL.revokeObjectURL(downloadUrl);

			console.log(`ファイル ${filename} のダウンロードが開始されました。`);
		} else {
			console.error('期待されるBlobデータがレスポンスに含まれていません。');
		}

	} catch (error) {
		if (axios.isAxiosError(error)) {
			console.error('ファイルのダウンロード中にエラーが発生しました:', error.message);
			if (error.response) {
				console.error('Status:', error.response.status);
				if (error.response.data instanceof Blob) {
					const reader = new FileReader();
					reader.onload = () => {
						console.error('Error Body:', reader.result);
					};
					reader.readAsText(error.response.data);
				} else {
					console.error('Error Body:', error.response.data);
				}
			}
		} else {
			console.error('予期せぬエラー:', error);
		}
	}
}

async function onFightingStrengthUpload(): Promise<void> {
	const formData: FormData = new FormData();

	const fileElement = document.getElementById(
		"fightingStrengthFile"
	) as HTMLInputElement;
	const multipartFileList: FileList = fileElement.files as FileList;
	if (multipartFileList && multipartFileList.length !== 0) {
		for (let i = 0; i < multipartFileList.length; i++) {
			formData.append('multipartFiles', multipartFileList[i]);
		}
	}
	await axios.post("/fightingStrength/excel", formData, {
		headers: {
			'Content-Type': 'multipart/form-data'
		}
	}).then(function(response) {
	});
}

async function onDeleteMember(): Promise<void> {
	const deleteInput = document.getElementById("deleteInput") as HTMLInputElement;
	const memberIdToDelete: number = parseInt(deleteInput.value, 10);
	await axios.delete(`/member/` + memberIdToDelete);
	fetchAndDisplayMember();
}
async function onSaveFightingStrength(): Promise<void> {
	const textElement = document.getElementById(
		"fightingStrengthMessage"
	) as HTMLTextAreaElement;
	const text: string = textElement.value as string;
	const formData: FormData = new FormData();
	formData.append("text", text);

	// Axiosでリクエスト送信
	await axios.put("/fightingStrength", formData, {
	}).then(function(response) {
	});

}
async function onAddMember(): Promise<void> {
	const ayarabuNameInput = document.getElementById("ayarabuNameInput") as HTMLInputElement; // 変数名をdeleteInputからayarabuInputに修正
	if (!ayarabuNameInput) { // 要素が存在しない場合のエラーを避ける
		console.error("ayarabuNameInput element not found.");
		return;
	}
	const ayarabuIdInput = document.getElementById("ayarabuIdInput") as HTMLInputElement;
	const allianceSelect = document.getElementById("allianceSelect") as HTMLSelectElement;
	const memberData: AllianceMemberForm = {
		id: -1, // 新規追加のため仮のID
		memberRole: "MEMBER",
		discordMemberId: "",
		discordName: "",
		ayarabuId: ayarabuIdInput.value,
		ayarabuName: ayarabuNameInput.value,
		alliance: allianceSelect.value,
		statementCount: 0,
		createDate: "",
		bot: 0
	};
	await axios.post(`/member`, memberData);
	fetchAndDisplayMember();
}
interface AllianceMemberForm {
	id: number;
	memberRole: string;
	discordMemberId: string;
	discordName: string;
	ayarabuId: string;
	ayarabuName: string;
	alliance: string;
	statementCount: number;
	createDate: string;
	bot: number;
}


// --- 1. 取得するデータの型定義 ---
// APIから返されるユーザーデータの構造を定義します
interface Channel {
	id: number;
	channelId: string;
	channelName: string;
	chatMessageList: ChatMessageDto[];
}

interface ChatMessageDto {
	id: number;
	discordMessageId: string;
	quoteId: string;
	quoteDiscordId: string;
	channelId: string;
	channelName: string;
	name: string;
	message: string;
	chatAttachmentDtoList: ChatAttachmentDto[];
	createDate: string;
}

interface ChatAttachmentDto {
	attachmentUrl: string;
	attachmentFileName: string;
}

interface MessageSize {
	size: number;
}

// --- 2. DOM要素の取得 ---
// データを表示するコンテナ要素を取得します

// --- 3. Axiosで全件データ取得とDOM操作を行う非同期関数 ---
async function fetchAndDisplayChatMessage(
	resetFlag: boolean = false
): Promise<void> {
	// Axiosを使って全ユーザーデータを取得します
	// レスポンスのデータ部分の型を `User[]` (Userオブジェクトの配列) として指定します
	if (page === 0 || resetFlag) {
		page = 0;
		chatMessageContainer.innerHTML = "";

		loadingMoreIndicator.innerHTML = "ロード中...";
	}
	const response = await axios.get<ChatMessageDto[]>(
		`/chat/pageable?page=${page}&size=${size}&channelId=${channelId}&sort=id,DESC`
	);
	const chatMessages: ChatMessageDto[] = response.data; // 取得したデータ

	const loading = document.createElement("div");
	// 読み込み中のメッセージをクリアします
	// 取得したユーザーデータを元にDOMを操作します
	chatMessages.forEach((chatMessage) => {
		if (chatMessage.channelId != channelId)
			return;
		// --- 4. DOM操作: ユーザーごとにカード要素を作成 ---
		const messageDiv = document.createElement("div") as HTMLDivElement;
		messageDiv.className = "messageDiv";
		messageDiv.id = chatMessage.discordMessageId;

		const head = document.createElement("button") as HTMLButtonElement;
		head.addEventListener("click", onHeadClick);
		const idElement = document.createElement("div") as HTMLDivElement;
		head.appendChild(idElement);
		head.textContent = `id: ${chatMessage.id} 名前: ${chatMessage.name} ${chatMessage.createDate}`;
		head.className = "headDiv";
		head.id = `${chatMessage.id}`;

		const main = document.createElement("div") as HTMLDivElement;
		main.className = "mainDiv";
		const quate = document.createElement("div") as HTMLDivElement;
		quate.className = "quateDiv";
		const message = document.createElement("div") as HTMLDivElement;
		if (chatMessage.quoteId != null) {
			quate.innerHTML = `>> <a href="#${chatMessage.quoteDiscordId}">${chatMessage.quoteId}</a>`;
			main.appendChild(quate);
		}
		let mainContents: string = "";
		mainContents += `${chatMessage.message}<br>`;
		message.innerHTML = mainContents;
		main.appendChild(message);
		if (
			chatMessage.chatAttachmentDtoList != null &&
			chatMessage.chatAttachmentDtoList.length != 0
		) {
			chatMessage.chatAttachmentDtoList.forEach((chatAttachmentDto) => {
				const a = document.createElement("a");
				a.href = chatAttachmentDto.attachmentUrl;
				if (chatAttachmentDto.attachmentUrl.match(".jpg|.png|.JPG|.PNG") != null) {
					const divEle = document.createElement("div")as HTMLDivElement;
					divEle.innerHTML = `
						<a href="${chatAttachmentDto.attachmentUrl}" data-lightbox="image-1" data-title="title" data-alt="alt">
						<img src="${chatAttachmentDto.attachmentUrl}" width=300>
						</a>
					    `;
					main.appendChild(divEle);
				} else {
					const p = document.createElement("p") as HTMLElement;
					p.textContent = "リンク";
					a.appendChild(p);
				}
				main.appendChild(a);
			});
		}

		// 作成した要素をユーザーカードに追加
		messageDiv.appendChild(head);
		messageDiv.appendChild(main);
		// ユーザーカードをコンテナに追加
		chatMessageContainer.appendChild(messageDiv);
		page++;
	});
	loadingMoreIndicator.innerHTML = "";
}

async function onPostMessage(): Promise<void> {
	// FormDataオブジェクトを作成
	const formData: FormData = new FormData();

	const nameElement = document.getElementById("name") as HTMLInputElement;
	const name: string = nameElement.value as string;
	const messageElement = document.getElementById(
		"message"
	) as HTMLTextAreaElement;
	const message: string = messageElement.value as string;
	const referencedMessageIdElement = document.getElementById(
		"referencedMessageId"
	) as HTMLInputElement;
	const referencedMessageId: string =
		referencedMessageIdElement.value as string;
	const fileElement = document.getElementById(
		"multipartFile"
	) as HTMLInputElement;
	const multipartFileList: FileList = fileElement.files as FileList;
	if (multipartFileList && multipartFileList.length !== 0) {
		for (let i = 0; i < multipartFileList.length; i++) {
			formData.append('multipartFiles', multipartFileList[i]);
		}
	}

	formData.append("channelId", channelId);
	formData.append("name", name);
	formData.append("message", message);
	formData.append("referencedMessageId", referencedMessageId);

	// Axiosでリクエスト送信
	await axios.post("/chat", formData, {
		headers: {
			'Content-Type': 'multipart/form-data'
		}
	}).then(function(response) {
		window.setTimeout(resetAndDisplayChatMessage, 500);
	});
}

function onHeadClick(event: MouseEvent): void {
	const clickedElement = event.target as HTMLElement;
	const referencedMessageIdElement = document.getElementById(
		"referencedMessageId"
	) as HTMLInputElement;
	const headElement: HTMLInputElement = document.getElementById(
		clickedElement.id
	) as HTMLInputElement;
	referencedMessageIdElement.value = clickedElement.id;
	headElement.style.display = "block";
}

async function checkNewMessage(): Promise<void> {
	const messageSizeResponse = await axios.get<MessageSize>(`/chat/size`);
	const newSize = messageSizeResponse.data.size;
	if (newSize > maxSize) {
		resetAndDisplayChatMessage();
	}
}

async function resetAndDisplayChatMessage(): Promise<void> {
	page = 0;
	fetchAndDisplayChatMessage(true);
}

// メンバー用ソース
interface Member {
	id: number;
	memberRole: string;
	discordMemberId: string;
	discordName: string;
	ayarabuId: string;
	ayarabuName: string;
	alliance: string;
	statementCount: number;
	createDate: string;
	bot: number;
}
async function fetchAndDisplayMember(): Promise<void> {
	const response = await axios.get<Member[]>(`/member`);
	const members: Member[] = response.data; // 取得したデータ
	const memberRoles: string[] = ["LEADER", "SUB_LEADER", "MEMBER"];
	const memberAlliance: string[] = ["HOKKORI", "HONTO_HOKKORI", "NONE"];

	const memberTable = document.querySelector(
		"#memberTable tbody"
	) as HTMLElement;
	memberTable.innerHTML = "";
	members.forEach((member) => {
		const row = document.createElement("tr") as HTMLTableRowElement;
		// セルのidを設定して、更新時にどの行のデータか識別できるようにする
		row.dataset.id = member.id + "";
		const roleOptions = memberRoles
			.map(
				(role) =>
					`<option value="${role}" ${member.memberRole === role ? "selected" : ""
					}>${role}</option>`
			)
			.join("");
		const allianceOptions = memberAlliance
			.map(
				(alliance) =>
					`<option value="${alliance}" ${member.alliance === alliance ? "selected" : ""
					}>${alliance}</option>`
			)
			.join("");
		row.innerHTML = `
	                    <td>${member.id}</td>
						<td><select id="memberRoleSelect${member.id}">${roleOptions}</select></td>
						<td contenteditable="true">${member.discordMemberId}</td>
						<td contenteditable="true">${member.discordName}</td>
						<td contenteditable="true">${member.ayarabuId}</td>
						<td contenteditable="true">${member.ayarabuName}</td>
						<td><select id="memberAllianceSelect${member.id}">${allianceOptions}</select></td>
	                    <td>${member.statementCount}</td>
						<td>${member.createDate}</td>
						<td>${member.bot}</td>
	                `;

		memberTable.appendChild(row);
	});
}

async function onSaveMember(): Promise<void> {
	const table: HTMLTableElement = document.getElementById("memberTable") as HTMLTableElement;
	const tableRows: HTMLCollectionOf<HTMLTableRowElement> = table.rows;

	// 要素を一つずつ処理
	for (let i = 1; i < tableRows.length; i++) {
		const row: HTMLTableRowElement = tableRows.item(i) as HTMLTableRowElement; // または listItems[i]
		const id: number = parseInt(row.cells[0].textContent || "0", 10);

		const memberRoleElement: HTMLSelectElement = document.getElementById(`memberRoleSelect${id}`) as HTMLSelectElement;
		const memberRoleStr: string = memberRoleElement.value;
		const allianceElement = document.getElementById(`memberAllianceSelect${id}`) as HTMLSelectElement;
		const allianceStr = allianceElement.value;

		const updateMember: AllianceMemberForm = {
			id: id,
			memberRole: memberRoleStr,
			discordMemberId: row.cells[2].textContent || "",
			discordName: row.cells[3].textContent || "",
			ayarabuId: row.cells[4].textContent || "",
			ayarabuName: row.cells[5].textContent || "",
			alliance: allianceStr,
			statementCount: parseInt(row.cells[7].textContent || "0", 10),
			createDate: row.cells[8].textContent || "",
			bot: parseInt(row.cells[9].textContent || "0", 10)
		};

		console.log("updateMEmber:", updateMember);
		await axios.put<AllianceMemberForm>(`/member`, updateMember, {}).then(function(response) {
			fetchAndDisplayMember();
		});
	}

}



