(() => {
  "use strict";

  const DEFAULTS = { mistEnabled: true, showRules: true };

  const MIN_NAME = 2;
  const PLAYER_COUNT = 3;
  const MAX_HUMAN_PLAYERS = 4;
  const MAX_PLAYERS = 5;
  const MODES = ["ai", "human"];

  const params = new URLSearchParams(location.search);

  const readBool = (value, fallback) =>
    value === null ? fallback : !/^(0|false|no|nao|não)$/i.test(value.trim());

  const config = {
    mistEnabled: readBool(params.get("mist"), DEFAULTS.mistEnabled),
    showRules: readBool(params.get("rules"), DEFAULTS.showRules)
  };

  const state = {
    names: Array(MAX_HUMAN_PLAYERS).fill(""),
    humanCount: 1,
    aiCount: 1,
    mode: null,
    started: false,
    page: "setup"
  };

  const el = {
    mist: document.querySelector("[data-mist]"),
    logo: document.querySelector("[data-logo]"),
    logoFallback: document.querySelector("[data-logo-fallback]"),
    playerFields: document.querySelector("[data-player-fields]"),
    playerNames: Array.from(document.querySelectorAll("[data-player-name]")),
    aiConfig: document.querySelector("[data-ai-config]"),
    humanCount: document.querySelector("[data-human-count]"),
    aiCount: document.querySelector("[data-ai-count]"),
    humanFields: Array.from(document.querySelectorAll("[data-human-field]")),
    humanNames: Array.from(document.querySelectorAll("[data-human-name]")),
    modes: document.querySelector("[data-modes]"),
    modeButtons: Array.from(document.querySelectorAll("[data-mode]:not(:disabled)")),
    play: document.querySelector("[data-play]"),
    hint: document.querySelector("[data-hint]"),
    links: document.querySelector("[data-links]"),
    setup: document.querySelector('[data-view="setup"]'),
    start: document.querySelector('[data-view="start"]'),
    infoHow: document.querySelector('[data-view="how"]'),
    infoRules: document.querySelector('[data-view="rules"]'),
    infoBackButtons: Array.from(document.querySelectorAll("[data-info-back]")),
    infoPageButtons: Array.from(document.querySelectorAll("[data-info-page]")),
    startName: document.querySelector("[data-start-name]"),
    summary: document.querySelector("[data-summary]"),
    back: document.querySelector("[data-back]")
  };


  const isReady = () => state.mode === "ai"
    ? state.names.slice(0, state.humanCount)
      .every((name) => name.trim().length >= MIN_NAME)
    : state.mode === "human"
      && state.names.slice(0, PLAYER_COUNT).every((name) => name.trim().length >= MIN_NAME);

  function summaryFor(mode) {
    switch (mode) {
      case "human":
        return "Partida local para três pessoas, em turnos no mesmo dispositivo.";
      case "ai":
        return `Partida com ${state.humanCount} ${state.humanCount === 1 ? "jogador humano" : "jogadores humanos"} e ${state.aiCount} ${state.aiCount === 1 ? "IA" : "IAs"} (${state.humanCount + state.aiCount} participantes).`;
      default:
        return "";
    }
  }

  function hintFor() {
    if (!state.mode) return "Escolha o formato da partida.";
    if (isReady()) return "";
    return state.mode === "ai"
      ? "Informe o nome de cada jogador (mínimo de 2 caracteres)."
      : "Preencha os três nomes (mínimo de 2 caracteres cada).";
  }

  function focusSetup() {
    if (state.mode === "human") {
      el.playerNames[0].focus();
    } else if (state.mode === "ai") {
      el.humanNames[0].focus();
    } else {
      el.modeButtons[0].focus();
    }
  }

  function render() {
    const showingInfo = state.page === "how" || state.page === "rules";
    el.setup.hidden = state.started;
    el.setup.classList.toggle("view--reserved", showingInfo);
    el.setup.setAttribute("aria-hidden", String(showingInfo));
    el.setup.inert = showingInfo;
    el.start.hidden = !state.started;
    el.playerFields.hidden = state.mode !== "human";
    el.aiConfig.hidden = state.mode !== "ai";
    el.humanFields.forEach((field, index) => {
      field.hidden = index >= state.humanCount;
    });
    el.infoHow.hidden = state.started || state.page !== "how";
    el.infoRules.hidden = state.started || state.page !== "rules";

    el.modeButtons.forEach((button) => {
      const selected = button.dataset.mode === state.mode;
      button.setAttribute("aria-checked", String(selected));
      button.tabIndex = selected ? 0 : -1;
    });

    if (!state.mode && el.modeButtons.length) el.modeButtons[0].tabIndex = 0;

    el.play.disabled = !isReady();
    el.hint.textContent = hintFor();

    el.startName.textContent = state.mode === "ai"
      ? [
        ...state.names.slice(0, state.humanCount).map((name) => name.trim()),
        ...Array.from({ length: state.aiCount }, (_, index) => `IA ${index + 1}`)
      ].join(" · ")
      : state.names.slice(0, PLAYER_COUNT).map((name) => name.trim()).join(" · ");
    el.summary.textContent = summaryFor(state.mode);
  }

  function selectMode(mode) {
    if (!MODES.includes(mode)) return;
    state.mode = mode;
    render();
  }

  async function play() {
    if (!isReady() || state.started) return;
    state.started = true;
    render();
    el.back.focus();

    const detail = {
      players: state.mode === "ai"
        ? state.names.slice(0, state.humanCount).map((name) => name.trim())
        : state.names.slice(0, PLAYER_COUNT).map((name) => name.trim()),
      quantidadeIAs: state.mode === "ai" ? state.aiCount : 0,
      mode: state.mode
    };
    document.dispatchEvent(new CustomEvent("carcassonne:start", { detail }));

    // O modelo Java cria a partida; a tela de jogo só lê o estado pronto.
    try {
      const resposta = await fetch("/api/partida", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          jogadores: detail.players,
          quantidadeIAs: detail.quantidadeIAs,
          modo: detail.mode
        })
      });
      if (!resposta.ok) throw new Error("o servidor recusou a partida");
      window.location.href = "jogo.html";
    } catch (erro) {
      state.started = false;
      render();
      el.hint.textContent =
        "Não foi possível falar com o servidor. No Windows, rode .\\run.bat; no macOS/Linux, rode ./run.sh. Depois abra http://localhost:8080.";
    }
  }

  function back() {
    state.started = false;
    state.page = "setup";
    render();
    focusSetup();
  }

  function showInfo(page) {
    if (page !== "how" && page !== "rules") return;
    state.page = page;
    render();
    (page === "how" ? el.infoHow : el.infoRules).querySelector(".info__title").focus();
  }

  function backToSetup() {
    state.page = "setup";
    render();
    focusSetup();
  }

  el.playerNames.forEach((input, index) => {
    input.addEventListener("input", () => {
      state.names[index] = input.value;
      render();
    });
    input.addEventListener("keydown", (event) => {
      if (event.key === "Enter") {
        event.preventDefault();
        if (index < el.playerNames.length - 1) {
          el.playerNames[index + 1].focus();
        } else if (isReady()) {
          play();
        }
      }
    });
  });

  el.humanNames.forEach((input, index) => {
    input.addEventListener("input", () => {
      state.names[index] = input.value;
      render();
    });
    input.addEventListener("keydown", (event) => {
      if (event.key !== "Enter") return;
      event.preventDefault();
      const proximo = el.humanNames.slice(index + 1, state.humanCount)[0];
      if (proximo) {
        proximo.focus();
      } else if (isReady()) {
        play();
      }
    });
  });

  el.humanCount.addEventListener("change", () => {
    state.humanCount = Number(el.humanCount.value);
    state.aiCount = Math.min(state.aiCount, MAX_PLAYERS - state.humanCount);
    el.aiCount.value = String(state.aiCount);
    render();
  });
  el.aiCount.addEventListener("change", () => {
    state.aiCount = Number(el.aiCount.value);
    state.humanCount = Math.min(state.humanCount, MAX_PLAYERS - state.aiCount);
    el.humanCount.value = String(state.humanCount);
    render();
  });

  el.modeButtons.forEach((button) => {
    button.addEventListener("click", () => selectMode(button.dataset.mode));
  });

  el.modes.addEventListener("keydown", (event) => {
    const step = { ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 }[event.key];
    if (!step) return;
    event.preventDefault();
    const current = el.modeButtons.findIndex((b) => b === document.activeElement);
    const next = el.modeButtons[(Math.max(0, current) + step + el.modeButtons.length) % el.modeButtons.length];
    selectMode(next.dataset.mode);
    next.focus();
  });

  el.play.addEventListener("click", play);
  el.back.addEventListener("click", back);

  el.links.addEventListener("click", (event) => {
    const link = event.target.closest("a[data-link]");
    if (!link) return;
    event.preventDefault();
    showInfo(link.dataset.link);
  });

  el.infoBackButtons.forEach((button) => button.addEventListener("click", backToSetup));
  el.infoPageButtons.forEach((button) => {
    button.addEventListener("click", () => showInfo(button.dataset.infoPage));
  });

  if (!config.mistEnabled) el.mist.remove();
  if (!config.showRules) el.links.remove();

  const useLogoFallback = () => {
    el.logo.hidden = true;
    el.logoFallback.hidden = false;
  };

  el.logo.addEventListener("error", useLogoFallback);
  if (el.logo.complete && el.logo.naturalWidth === 0) useLogoFallback();

  const background = new Image();
  background.addEventListener("load", () => {
    document.documentElement.classList.add("has-art");
  });
  background.src = "assets/marca/carcassoneimagemfinal.png";

  render();
})();
