package com.codex.pestcontrol.core;

import java.util.Collections;
import java.util.List;

/** What the Pilot says to do right now in a game. */
public final class Advice
{
	public enum Kind
	{
		/** A portal's shield is down: go and attack it. */
		ATTACK,
		/** No shield is down yet, but the next one to drop is known: make for it. */
		NEXT,
		/** No shield is down and the next could be one of several: hold by the knight. */
		WAIT,
		/** The Void Knight is in danger: defend him. */
		GUARD,
		/** Every portal has fallen. */
		DONE
	}

	private final Kind kind;
	private final Portal portal;
	private final List<Portal> candidates;
	private final String headline;
	private final String detail;

	public Advice(Kind kind, Portal portal, List<Portal> candidates, String headline, String detail)
	{
		this.kind = kind;
		this.portal = portal;
		this.candidates = candidates == null ? Collections.emptyList() : Collections.unmodifiableList(candidates);
		this.headline = headline;
		this.detail = detail;
	}

	public Kind getKind()
	{
		return kind;
	}

	/** The portal to work, or null when the advice is not about one portal. */
	public Portal getPortal()
	{
		return portal;
	}

	/** Every portal the advice could be about, such as the ones whose shield might drop next. */
	public List<Portal> getCandidates()
	{
		return candidates;
	}

	public String getHeadline()
	{
		return headline;
	}

	public String getDetail()
	{
		return detail;
	}

	@Override
	public String toString()
	{
		return kind + " " + portal + ": " + headline + " (" + detail + ")";
	}
}
